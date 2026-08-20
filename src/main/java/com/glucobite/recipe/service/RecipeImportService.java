package com.glucobite.recipe.service;

import com.glucobite.auth.exception.InvalidCredentialsException;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.ingredient.repository.IngredientRepository;
import com.glucobite.recipe.dto.ImportedRecipeResponse;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.dto.RecipeIngredientResponse;
import com.glucobite.recipe.dto.RecipeStepResponse;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeImportType;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.entity.RecipeStep;
import com.glucobite.recipe.exception.InvalidRecipeAnalysisException;
import com.glucobite.recipe.importing.AnalyzedRecipe;
import com.glucobite.recipe.importing.RecipeTextAnalyzer;
import com.glucobite.recipe.importing.RecipeSourceMetadata;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RecipeImportService {

    private static final int MAX_INGREDIENTS = 50;
    private static final int MAX_STEPS = 100;
    private static final BigDecimal MAX_AMOUNT_GRAMS = new BigDecimal("100000.00");
    private static final BigDecimal MAX_NUTRITION_PER_GRAM = new BigDecimal("1000000.00");

    private final RecipeTextAnalyzer analyzer;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientNutritionRepository ingredientNutritionRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeNutritionCalculator nutritionCalculator;
    private final TransactionTemplate transactionTemplate;

    public RecipeImportService(
            RecipeTextAnalyzer analyzer,
            UserRepository userRepository,
            RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository,
            IngredientNutritionRepository ingredientNutritionRepository,
            RecipeIngredientRepository recipeIngredientRepository,
            RecipeStepRepository recipeStepRepository,
            RecipeNutritionCalculator nutritionCalculator,
            TransactionTemplate transactionTemplate
    ) {
        this.analyzer = analyzer;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientNutritionRepository = ingredientNutritionRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recipeStepRepository = recipeStepRepository;
        this.nutritionCalculator = nutritionCalculator;
        this.transactionTemplate = transactionTemplate;
    }

    public ImportedRecipeResponse importText(Long userId, String sourceText) {
        return importAnalyzedText(
                userId,
                sourceText,
                RecipeImportType.TEXT,
                RecipeSourceMetadata.none()
        );
    }

    public ImportedRecipeResponse importAnalyzedText(
            Long userId,
            String sourceText,
            RecipeImportType importType,
            RecipeSourceMetadata sourceMetadata
    ) {
        AnalyzedRecipe analyzed = analyzer.analyze(sourceText);
        validate(analyzed);
        ImportedRecipeResponse response = transactionTemplate.execute(status ->
                persist(userId, analyzed, importType, sourceMetadata)
        );
        if (response == null) {
            throw new IllegalStateException("레시피 저장 결과가 없습니다.");
        }
        return response;
    }

    private ImportedRecipeResponse persist(
            Long userId,
            AnalyzedRecipe analyzed,
            RecipeImportType importType,
            RecipeSourceMetadata sourceMetadata
    ) {
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        List<ResolvedIngredient> resolvedIngredients = resolveIngredients(analyzed.ingredients());
        NutritionSummary totalNutrition = nutritionCalculator.sum(resolvedIngredients.stream()
                .map(item -> nutritionCalculator.contribute(item.nutrition(), item.amount()))
                .toList());

        Recipe recipe = recipeRepository.save(new Recipe(
                user,
                analyzed.title().trim(),
                trimToNull(analyzed.description()),
                analyzed.cookingTime(),
                importType,
                scale(totalNutrition.calories())
        ));
        recipe.attachSourceMetadata(
                sourceMetadata.sourceUrl(),
                sourceMetadata.externalId(),
                sourceMetadata.imageUrl()
        );
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.saveAll(
                resolvedIngredients.stream()
                        .map(item -> new RecipeIngredient(recipe, item.ingredient(), scale(item.amount())))
                        .toList()
        );
        List<RecipeStep> steps = new ArrayList<>();
        for (int index = 0; index < analyzed.steps().size(); index++) {
            steps.add(new RecipeStep(recipe, index + 1, analyzed.steps().get(index).trim()));
        }
        List<RecipeStep> savedSteps = recipeStepRepository.saveAll(steps);

        return new ImportedRecipeResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getCookingTime(),
                recipe.getImportType(),
                recipe.getRecipeType(),
                recipe.isCompleted(),
                recipe.getSourceUrl(),
                recipe.getSourceExternalId(),
                recipe.getImageUrl(),
                scale(totalNutrition),
                recipeIngredients.stream()
                        .map(item -> new RecipeIngredientResponse(
                                item.getIngredient().getId(),
                                item.getIngredient().getTitle(),
                                item.getAmount()
                        ))
                        .toList(),
                savedSteps.stream().map(RecipeStepResponse::from).toList()
        );
    }

    private List<ResolvedIngredient> resolveIngredients(
            List<AnalyzedRecipe.IngredientData> analyzedIngredients
    ) {
        Map<String, MergedIngredient> merged = new LinkedHashMap<>();
        for (AnalyzedRecipe.IngredientData item : analyzedIngredients) {
            String title = normalizeTitle(item.title());
            String key = title.toLowerCase(Locale.ROOT);
            merged.merge(
                    key,
                    new MergedIngredient(title, item.amountGrams(), item.nutritionPerGram()),
                    (left, right) -> new MergedIngredient(
                            left.title(),
                            left.amount().add(right.amount()),
                            left.nutrition()
                    )
            );
        }

        List<ResolvedIngredient> resolved = new ArrayList<>();
        for (MergedIngredient item : merged.values()) {
            Ingredient ingredient = ingredientRepository.findFirstByTitleIgnoreCase(item.title())
                    .orElseGet(() -> ingredientRepository.save(new Ingredient(item.title())));
            IngredientNutrition nutrition = ingredientNutritionRepository.findByIngredientId(ingredient.getId())
                    .orElseGet(() -> ingredientNutritionRepository.save(toEntity(
                            ingredient, item.nutrition()
                    )));
            resolved.add(new ResolvedIngredient(ingredient, nutrition, item.amount()));
        }
        return resolved;
    }

    private IngredientNutrition toEntity(Ingredient ingredient, NutritionSummary nutrition) {
        return new IngredientNutrition(
                ingredient,
                scale(nutrition.calories()),
                scale(nutrition.carb()),
                scale(nutrition.protein()),
                scale(nutrition.fat()),
                scale(nutrition.fiber()),
                scale(nutrition.sugar()),
                scale(nutrition.sodium())
        );
    }

    private void validate(AnalyzedRecipe analyzed) {
        if (analyzed == null) {
            throw invalid("레시피 분석 결과가 비어 있습니다.");
        }
        requireText(analyzed.title(), 150, "레시피 제목");
        if (analyzed.cookingTime() == null
                || analyzed.cookingTime() < 1
                || analyzed.cookingTime() > 1_440) {
            throw invalid("조리 시간은 1분 이상 1,440분 이하여야 합니다.");
        }
        if (analyzed.ingredients() == null
                || analyzed.ingredients().isEmpty()
                || analyzed.ingredients().size() > MAX_INGREDIENTS) {
            throw invalid("재료는 1개 이상 50개 이하여야 합니다.");
        }
        if (analyzed.steps() == null
                || analyzed.steps().isEmpty()
                || analyzed.steps().size() > MAX_STEPS) {
            throw invalid("조리 단계는 1개 이상 100개 이하여야 합니다.");
        }
        for (AnalyzedRecipe.IngredientData ingredient : analyzed.ingredients()) {
            requireText(ingredient.title(), 100, "재료명");
            requirePositiveWithin(ingredient.amountGrams(), MAX_AMOUNT_GRAMS, "재료 사용량");
            validateNutrition(ingredient.nutritionPerGram());
        }
        for (String step : analyzed.steps()) {
            requireText(step, 20_000, "조리 단계");
        }
    }

    private void validateNutrition(NutritionSummary nutrition) {
        if (nutrition == null) {
            throw invalid("재료 영양정보가 비어 있습니다.");
        }
        requireNonNegativeWithin(nutrition.calories(), "칼로리");
        requireNonNegativeWithin(nutrition.carb(), "탄수화물");
        requireNonNegativeWithin(nutrition.protein(), "단백질");
        requireNonNegativeWithin(nutrition.fat(), "지방");
        requireNonNegativeWithin(nutrition.fiber(), "식이섬유");
        requireNonNegativeWithin(nutrition.sugar(), "당류");
        requireNonNegativeWithin(nutrition.sodium(), "나트륨");
    }

    private void requireNonNegativeWithin(BigDecimal value, String field) {
        if (value == null
                || value.signum() < 0
                || value.compareTo(MAX_NUTRITION_PER_GRAM) > 0) {
            throw invalid(field + " 값이 올바르지 않습니다.");
        }
    }

    private void requirePositiveWithin(BigDecimal value, BigDecimal maximum, String field) {
        if (value == null || value.signum() <= 0 || value.compareTo(maximum) > 0) {
            throw invalid(field + " 값이 올바르지 않습니다.");
        }
    }

    private void requireText(String value, int maximum, String field) {
        if (value == null || value.isBlank() || value.trim().length() > maximum) {
            throw invalid(field + " 값이 올바르지 않습니다.");
        }
    }

    private String normalizeTitle(String title) {
        return title.trim().replaceAll("\\s+", " ");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private NutritionSummary scale(NutritionSummary nutrition) {
        return new NutritionSummary(
                scale(nutrition.calories()),
                scale(nutrition.carb()),
                scale(nutrition.protein()),
                scale(nutrition.fat()),
                scale(nutrition.fiber()),
                scale(nutrition.sugar()),
                scale(nutrition.sodium())
        );
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private InvalidRecipeAnalysisException invalid(String message) {
        return new InvalidRecipeAnalysisException(message);
    }

    private record MergedIngredient(
            String title,
            BigDecimal amount,
            NutritionSummary nutrition
    ) {
    }

    private record ResolvedIngredient(
            Ingredient ingredient,
            IngredientNutrition nutrition,
            BigDecimal amount
    ) {
    }
}
