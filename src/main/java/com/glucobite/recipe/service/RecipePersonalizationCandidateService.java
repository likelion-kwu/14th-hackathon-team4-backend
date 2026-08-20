package com.glucobite.recipe.service;

import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.ingredient.repository.IngredientRepository;
import com.glucobite.recipe.dto.GeneratePersonalizedRecipeRequest;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.dto.PersonalizedIngredientResponse;
import com.glucobite.recipe.dto.PersonalizedRecipeDetailResponse;
import com.glucobite.recipe.dto.RecipeIngredientResponse;
import com.glucobite.recipe.dto.RecipeStepResponse;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.entity.RecipeStep;
import com.glucobite.recipe.entity.RecipeType;
import com.glucobite.recipe.exception.RecipeNotFoundException;
import com.glucobite.recipe.exception.RecipeNotPersonalizableException;
import com.glucobite.recipe.exception.RecipePersonalizationGenerationException;
import com.glucobite.recipe.personalization.GeneratedPersonalization;
import com.glucobite.recipe.personalization.PersonalizationContext;
import com.glucobite.recipe.personalization.RecipePersonalizationGenerator;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecipePersonalizationCandidateService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientNutritionRepository ingredientNutritionRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final RecipeNutritionCalculator nutritionCalculator;
    private final DietaryRestrictionPolicy dietaryRestrictionPolicy;
    private final RecipePersonalizationGenerator generator;
    private final TransactionTemplate transactionTemplate;

    public RecipePersonalizationCandidateService(
            RecipeRepository recipeRepository,
            RecipeIngredientRepository recipeIngredientRepository,
            RecipeStepRepository recipeStepRepository,
            IngredientRepository ingredientRepository,
            IngredientNutritionRepository ingredientNutritionRepository,
            HealthProfileRepository healthProfileRepository,
            RecipeNutritionCalculator nutritionCalculator,
            DietaryRestrictionPolicy dietaryRestrictionPolicy,
            RecipePersonalizationGenerator generator,
            TransactionTemplate transactionTemplate
    ) {
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.recipeStepRepository = recipeStepRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientNutritionRepository = ingredientNutritionRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.nutritionCalculator = nutritionCalculator;
        this.dietaryRestrictionPolicy = dietaryRestrictionPolicy;
        this.generator = generator;
        this.transactionTemplate = transactionTemplate;
    }

    public PersonalizedRecipeDetailResponse generate(
            Long userId,
            Long recipeId,
            GeneratePersonalizedRecipeRequest request
    ) {
        Long previousCandidateId = request == null ? null : request.previousCandidateId();
        LoadedContext loaded = transactionTemplate.execute(status ->
                loadContext(userId, recipeId, previousCandidateId)
        );
        if (loaded == null) {
            throw new RecipePersonalizationGenerationException("개인화 입력을 준비하지 못했습니다.");
        }
        GeneratedPersonalization generated = generator.generate(loaded.context());
        PersonalizedRecipeDetailResponse response = transactionTemplate.execute(status ->
                persistCandidate(userId, recipeId, loaded.restrictedTerms(), generated)
        );
        if (response == null) {
            throw new RecipePersonalizationGenerationException("개인화 후보를 저장하지 못했습니다.");
        }
        return response;
    }

    private LoadedContext loadContext(Long userId, Long recipeId, Long previousCandidateId) {
        Recipe recipe = findOwnedRecipe(userId, recipeId);
        if (recipe.getRecipeType() != RecipeType.BASE || recipe.isCompleted()) {
            throw new RecipeNotPersonalizableException(
                    "개인화 후보는 completed=false인 BASE Recipe에서만 생성할 수 있습니다."
            );
        }
        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(HealthProfileNotFoundException::new);
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(recipeId);
        List<String> steps = recipeStepRepository.findByRecipeIdOrderByStepOrderAsc(recipeId).stream()
                .map(RecipeStep::getDescription)
                .toList();

        Map<Long, IngredientNutrition> nutritions = ingredientNutritionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        nutrition -> nutrition.getIngredient().getId(),
                        Function.identity()
                ));
        List<PersonalizationContext.CatalogIngredient> catalog = ingredientRepository.findAll().stream()
                .map(ingredient -> toCatalogIngredient(ingredient, nutritions.get(ingredient.getId())))
                .toList();
        PersonalizationContext.PreviousCandidate previous = previousCandidateId == null
                ? null
                : loadPreviousCandidate(userId, recipeId, previousCandidateId);
        PersonalizationContext context = new PersonalizationContext(
                new PersonalizationContext.RecipeData(
                        recipe.getTitle(),
                        recipe.getDescription(),
                        recipe.getCookingTime(),
                        recipeIngredients.stream()
                                .map(item -> new PersonalizationContext.RecipeIngredientData(
                                        item.getIngredient().getId(),
                                        item.getIngredient().getTitle(),
                                        item.getAmount()
                                ))
                                .toList(),
                        steps
                ),
                new PersonalizationContext.HealthData(
                        profile.getHealthGoal().name(),
                        profile.getDiabetesStatus() == null ? null : profile.getDiabetesStatus().name(),
                        profile.getDailyCarbsTarget(),
                        profile.getVegetarianType().name(),
                        profile.getAllergens().stream().map(allergen -> allergen.getName()).toList(),
                        profile.getDietaryRestrictionNote()
                ),
                catalog,
                previous
        );
        return new LoadedContext(context, dietaryRestrictionPolicy.restrictedTerms(profile));
    }

    private PersonalizationContext.PreviousCandidate loadPreviousCandidate(
            Long userId,
            Long sourceRecipeId,
            Long previousCandidateId
    ) {
        Recipe previous = findOwnedRecipe(userId, previousCandidateId);
        if (previous.getRecipeType() != RecipeType.PERSONALIZATION_CANDIDATE
                || previous.getSourceRecipe() == null
                || !previous.getSourceRecipe().getId().equals(sourceRecipeId)) {
            throw new RecipeNotPersonalizableException("직전 후보가 해당 BASE Recipe의 후보가 아닙니다.");
        }
        List<String> ingredientTitles = recipeIngredientRepository.findByRecipeId(previousCandidateId).stream()
                .map(item -> item.getIngredient().getTitle())
                .toList();
        return new PersonalizationContext.PreviousCandidate(previous.getTitle(), ingredientTitles);
    }

    private PersonalizedRecipeDetailResponse persistCandidate(
            Long userId,
            Long sourceRecipeId,
            Set<String> restrictedTerms,
            GeneratedPersonalization generated
    ) {
        Recipe source = findOwnedRecipe(userId, sourceRecipeId);
        validateGenerated(generated);

        Map<Long, Ingredient> catalog = ingredientRepository.findAllById(
                        generated.ingredients().stream()
                                .map(GeneratedPersonalization.IngredientAmount::ingredientId)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));
        if (catalog.size() != generated.ingredients().size()) {
            throw new RecipePersonalizationGenerationException(
                    "OpenAI가 등록되지 않은 ingredientId를 반환했습니다."
            );
        }
        for (Ingredient ingredient : catalog.values()) {
            if (dietaryRestrictionPolicy.isRestricted(ingredient, restrictedTerms)) {
                throw new RecipePersonalizationGenerationException(
                        "OpenAI 후보에 건강 제한 재료가 포함되었습니다."
                );
            }
        }

        List<RecipeIngredient> originalIngredients = recipeIngredientRepository.findByRecipeId(sourceRecipeId);
        Map<Long, IngredientNutrition> nutritions = loadNutritions(
                originalIngredients.stream().map(item -> item.getIngredient().getId()).toList(),
                generated.ingredients().stream()
                        .map(GeneratedPersonalization.IngredientAmount::ingredientId)
                        .toList()
        );
        NutritionSummary originalNutrition = calculateNutrition(originalIngredients, nutritions);
        NutritionSummary personalizedNutrition = nutritionCalculator.sum(generated.ingredients().stream()
                .map(item -> nutritionCalculator.contribute(
                        nutritions.get(item.ingredientId()), item.amount()
                ))
                .toList());

        Recipe candidate = Recipe.personalizationCandidate(
                source,
                generated.title().trim(),
                trimNullable(generated.description()),
                generated.cookingTime(),
                personalizedNutrition.calories().setScale(2, RoundingMode.HALF_UP),
                generated.label().trim(),
                generated.reason().trim(),
                generated.responseId()
        );
        recipeRepository.save(candidate);
        List<RecipeIngredient> candidateIngredients = generated.ingredients().stream()
                .map(item -> new RecipeIngredient(
                        candidate,
                        catalog.get(item.ingredientId()),
                        item.amount().setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();
        recipeIngredientRepository.saveAll(candidateIngredients);
        List<RecipeStep> candidateSteps = new ArrayList<>();
        for (int index = 0; index < generated.steps().size(); index++) {
            candidateSteps.add(new RecipeStep(candidate, index + 1, generated.steps().get(index).trim()));
        }
        recipeStepRepository.saveAll(candidateSteps);

        Set<Long> unchangedIngredientIds = unchangedIngredientIds(originalIngredients, candidateIngredients);
        return new PersonalizedRecipeDetailResponse(
                candidate.getId(),
                source.getId(),
                generated.label().trim(),
                generated.reason().trim(),
                candidate.getTitle(),
                candidate.getDescription(),
                candidate.getCookingTime(),
                originalNutrition,
                personalizedNutrition,
                nutritionCalculator.changes(originalNutrition, personalizedNutrition),
                originalIngredients.stream()
                        .map(item -> new RecipeIngredientResponse(
                                item.getIngredient().getId(),
                                item.getIngredient().getTitle(),
                                item.getAmount()
                        ))
                        .toList(),
                candidateIngredients.stream()
                        .map(item -> new PersonalizedIngredientResponse(
                                item.getIngredient().getId(),
                                item.getIngredient().getTitle(),
                                item.getAmount(),
                                !unchangedIngredientIds.contains(item.getIngredient().getId()),
                                unchangedIngredientIds.contains(item.getIngredient().getId())
                                        ? null
                                        : generated.reason().trim()
                        ))
                        .toList(),
                candidateSteps.stream().map(RecipeStepResponse::from).toList()
        );
    }

    private void validateGenerated(GeneratedPersonalization generated) {
        if (generated == null
                || isBlank(generated.responseId())
                || isBlank(generated.label())
                || isBlank(generated.title())
                || generated.title().length() > 150
                || isBlank(generated.reason())
                || generated.reason().length() > 500
                || generated.cookingTime() == null
                || generated.cookingTime() <= 0
                || generated.ingredients() == null
                || generated.ingredients().isEmpty()
                || generated.steps() == null
                || generated.steps().isEmpty()
                || generated.steps().stream().anyMatch(this::isBlank)) {
            throw new RecipePersonalizationGenerationException("OpenAI 개인화 후보 형식이 올바르지 않습니다.");
        }
        Set<Long> ids = new HashSet<>();
        for (GeneratedPersonalization.IngredientAmount item : generated.ingredients()) {
            if (item == null
                    || item.ingredientId() == null
                    || item.amount() == null
                    || item.amount().signum() <= 0
                    || !ids.add(item.ingredientId())) {
                throw new RecipePersonalizationGenerationException(
                        "OpenAI 개인화 후보의 재료 ID 또는 수량이 올바르지 않습니다."
                );
            }
        }
    }

    private Map<Long, IngredientNutrition> loadNutritions(
            Collection<Long> originalIds,
            Collection<Long> candidateIds
    ) {
        Set<Long> ids = new HashSet<>(originalIds);
        ids.addAll(candidateIds);
        return ingredientNutritionRepository.findByIngredientIdIn(ids).stream()
                .collect(Collectors.toMap(
                        nutrition -> nutrition.getIngredient().getId(),
                        Function.identity()
                ));
    }

    private NutritionSummary calculateNutrition(
            List<RecipeIngredient> ingredients,
            Map<Long, IngredientNutrition> nutritions
    ) {
        return nutritionCalculator.sum(ingredients.stream()
                .map(item -> nutritionCalculator.contribute(
                        nutritions.get(item.getIngredient().getId()), item.getAmount()
                ))
                .toList());
    }

    private Set<Long> unchangedIngredientIds(
            List<RecipeIngredient> originals,
            List<RecipeIngredient> candidates
    ) {
        Map<Long, BigDecimal> originalAmounts = originals.stream()
                .collect(Collectors.toMap(
                        item -> item.getIngredient().getId(),
                        RecipeIngredient::getAmount,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return candidates.stream()
                .filter(item -> {
                    BigDecimal originalAmount = originalAmounts.get(item.getIngredient().getId());
                    return originalAmount != null && originalAmount.compareTo(item.getAmount()) == 0;
                })
                .map(item -> item.getIngredient().getId())
                .collect(Collectors.toSet());
    }

    private PersonalizationContext.CatalogIngredient toCatalogIngredient(
            Ingredient ingredient,
            IngredientNutrition nutrition
    ) {
        return new PersonalizationContext.CatalogIngredient(
                ingredient.getId(),
                ingredient.getTitle(),
                nutrition == null ? null : nutrition.getCalories(),
                nutrition == null ? null : nutrition.getCarb(),
                nutrition == null ? null : nutrition.getProtein(),
                nutrition == null ? null : nutrition.getFat(),
                nutrition == null ? null : nutrition.getFiber()
        );
    }

    private Recipe findOwnedRecipe(Long userId, Long recipeId) {
        return recipeRepository.findByIdAndUserId(recipeId, userId)
                .orElseThrow(RecipeNotFoundException::new);
    }

    private String trimNullable(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record LoadedContext(PersonalizationContext context, Set<String> restrictedTerms) {
    }
}
