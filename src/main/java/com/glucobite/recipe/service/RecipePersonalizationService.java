package com.glucobite.recipe.service;

import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.entity.IngredientSubstitute;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.ingredient.repository.IngredientSubstituteRepository;
import com.glucobite.recipe.dto.ChangedIngredientResponse;
import com.glucobite.recipe.dto.IngredientSubstitutionRequest;
import com.glucobite.recipe.dto.IngredientAlternativeListResponse;
import com.glucobite.recipe.dto.IngredientAlternativeResponse;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.dto.PersonalizedIngredientResponse;
import com.glucobite.recipe.dto.RecipeIngredientResponse;
import com.glucobite.recipe.dto.RecipeSubstitutionPreviewResponse;
import com.glucobite.recipe.dto.RecipeSubstitutionRequest;
import com.glucobite.recipe.dto.SavePersonalizedRecipeRequest;
import com.glucobite.recipe.dto.SavedPersonalizedRecipeResponse;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.entity.RecipeStep;
import com.glucobite.recipe.exception.IngredientNotFoundException;
import com.glucobite.recipe.exception.InvalidRecipeSubstitutionException;
import com.glucobite.recipe.exception.InvalidSubstituteIngredientException;
import com.glucobite.recipe.exception.RecipeIngredientNotFoundException;
import com.glucobite.recipe.exception.RecipeNotFoundException;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecipePersonalizationService {

    private static final String FALLBACK_MANUAL_CHANGE_REASON =
            "선택한 대체 재료가 적용되었습니다.";

    private final RecipeRepository recipeRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientNutritionRepository ingredientNutritionRepository;
    private final IngredientSubstituteRepository ingredientSubstituteRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final RecipeNutritionCalculator nutritionCalculator;
    private final DietaryRestrictionPolicy dietaryRestrictionPolicy;

    public RecipePersonalizationService(
            RecipeRepository recipeRepository,
            RecipeStepRepository recipeStepRepository,
            RecipeIngredientRepository recipeIngredientRepository,
            IngredientNutritionRepository ingredientNutritionRepository,
            IngredientSubstituteRepository ingredientSubstituteRepository,
            HealthProfileRepository healthProfileRepository,
            RecipeNutritionCalculator nutritionCalculator,
            DietaryRestrictionPolicy dietaryRestrictionPolicy
    ) {
        this.recipeRepository = recipeRepository;
        this.recipeStepRepository = recipeStepRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientNutritionRepository = ingredientNutritionRepository;
        this.ingredientSubstituteRepository = ingredientSubstituteRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.nutritionCalculator = nutritionCalculator;
        this.dietaryRestrictionPolicy = dietaryRestrictionPolicy;
    }

    @Transactional(readOnly = true)
    public IngredientAlternativeListResponse getAlternatives(
            Long userId,
            Long recipeId,
            Long ingredientId
    ) {
        findOwnedRecipe(userId, recipeId);
        Set<String> restrictedTerms = dietaryRestrictionPolicy.restrictedTerms(findHealthProfile(userId));
        RecipeIngredient recipeIngredient = recipeIngredientRepository
                .findByRecipeIdAndIngredientId(recipeId, ingredientId)
                .orElseThrow(IngredientNotFoundException::new);
        Ingredient original = recipeIngredient.getIngredient();
        BigDecimal originalAmount = recipeIngredient.getAmount();
        IngredientNutrition originalNutrition = ingredientNutritionRepository
                .findByIngredientId(ingredientId)
                .orElse(null);
        NutritionSummary originalContribution = nutritionCalculator.contribute(
                recipeIngredient,
                originalNutrition
        );

        List<IngredientSubstitute> substitutes = ingredientSubstituteRepository
                .findByIngredientIdOrderByIdAsc(ingredientId).stream()
                .filter(substitute -> !dietaryRestrictionPolicy.isRestricted(
                        substitute.getSubstitute(), restrictedTerms
                ))
                .toList();
        Set<Long> substituteIngredientIds = substitutes.stream()
                .map(substitute -> substitute.getSubstitute().getId())
                .collect(Collectors.toSet());
        Map<Long, IngredientNutrition> substituteNutritions = loadNutritionMap(substituteIngredientIds);

        List<IngredientAlternativeResponse> alternatives = new ArrayList<>();
        for (IngredientSubstitute substitute : substitutes) {
            Ingredient substituteIngredient = substitute.getSubstitute();
            BigDecimal recommendedAmount = recommendedAmount(originalAmount, substitute.getRatio());
            IngredientNutrition substituteNutrition = substituteNutritions.get(substituteIngredient.getId());
            NutritionSummary substituteContribution = nutritionCalculator
                    .contribute(substituteNutrition, recommendedAmount);
            NutritionSummary changes = nutritionCalculator.changes(originalContribution, substituteContribution);
            alternatives.add(new IngredientAlternativeResponse(
                    substituteIngredient.getId(),
                    substituteIngredient.getTitle(),
                    recommendedAmount,
                    changes,
                    substitute.getReason()
            ));
        }
        return new IngredientAlternativeListResponse(
                recipeId,
                new RecipeIngredientResponse(original.getId(), original.getTitle(), originalAmount),
                alternatives
        );
    }

    @Transactional(readOnly = true)
    public RecipeSubstitutionPreviewResponse previewSubstitutions(
            Long userId,
            Long recipeId,
            RecipeSubstitutionRequest request
    ) {
        SubstitutionCalculation calculation = calculateSubstitutions(userId, recipeId, request);
        return new RecipeSubstitutionPreviewResponse(
                calculation.recipe().getId(),
                calculation.originalNutrition(),
                calculation.personalizedNutrition(),
                calculation.nutritionChanges(),
                calculation.changedIngredients(),
                toPersonalizedIngredientResponses(calculation.finalIngredients())
        );
    }

    @Transactional
    public SavedPersonalizedRecipeResponse savePersonalizedRecipe(
            Long userId,
            Long recipeId,
            SavePersonalizedRecipeRequest request
    ) {
        SubstitutionCalculation calculation = calculateSubstitutions(
                userId,
                recipeId,
                new RecipeSubstitutionRequest(request.substitutions())
        );
        Recipe source = calculation.recipe();
        String title = request.title() == null || request.title().isBlank()
                ? source.getTitle()
                : request.title();
        Recipe saved = Recipe.personalizedFrom(
                source,
                title,
                calculation.personalizedNutrition().calories().setScale(2, RoundingMode.HALF_UP)
        );
        recipeRepository.save(saved);

        List<RecipeIngredient> savedIngredients = calculation.finalIngredients().stream()
                .map(item -> new RecipeIngredient(
                        saved,
                        item.ingredient(),
                        item.amount(),
                        snapshot(item.nutritionPerGram())
                ))
                .toList();
        recipeIngredientRepository.saveAll(savedIngredients);

        List<RecipeStep> savedSteps = recipeStepRepository
                .findByRecipeIdOrderByStepOrderAsc(source.getId())
                .stream()
                .map(step -> new RecipeStep(saved, step.getStepOrder(), step.getDescription()))
                .toList();
        recipeStepRepository.saveAll(savedSteps);

        return new SavedPersonalizedRecipeResponse(
                saved.getId(),
                source.getId(),
                saved.getTitle(),
                calculation.personalizedNutrition()
        );
    }

    private SubstitutionCalculation calculateSubstitutions(
            Long userId,
            Long recipeId,
            RecipeSubstitutionRequest request
    ) {
        Recipe recipe = findOwnedRecipe(userId, recipeId);
        Set<String> restrictedTerms = dietaryRestrictionPolicy.restrictedTerms(findHealthProfile(userId));
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByRecipeId(recipeId);
        Map<Long, RecipeIngredient> ingredientsById = ingredients.stream()
                .collect(Collectors.toMap(
                        ingredient -> ingredient.getIngredient().getId(),
                        ingredient -> ingredient,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<Long, AppliedSubstitution> substitutionsByOriginalId = new LinkedHashMap<>();
        for (IngredientSubstitutionRequest item : request.substitutions()) {
            RecipeIngredient original = ingredientsById.get(item.originalIngredientId());
            if (original == null) {
                throw new RecipeIngredientNotFoundException();
            }
            if (substitutionsByOriginalId.containsKey(item.originalIngredientId())) {
                throw new InvalidRecipeSubstitutionException(
                        "동일한 원본 재료를 두 번 이상 대체할 수 없습니다."
                );
            }
            IngredientSubstitute registered = ingredientSubstituteRepository
                    .findByIngredientIdAndSubstituteId(
                            item.originalIngredientId(),
                            item.substituteIngredientId()
                    )
                    .orElseThrow(InvalidSubstituteIngredientException::new);
            if (dietaryRestrictionPolicy.isRestricted(registered.getSubstitute(), restrictedTerms)) {
                throw new InvalidSubstituteIngredientException();
            }
            substitutionsByOriginalId.put(
                    item.originalIngredientId(),
                    new AppliedSubstitution(registered, item.amount())
            );
        }

        Set<Long> nutritionIds = ingredients.stream()
                .map(ingredient -> ingredient.getIngredient().getId())
                .collect(Collectors.toSet());
        substitutionsByOriginalId.values().stream()
                .map(applied -> applied.registered().getSubstitute().getId())
                .forEach(nutritionIds::add);
        Map<Long, IngredientNutrition> nutritionMap = loadNutritionMap(nutritionIds);

        Set<Long> finalIngredientIds = new HashSet<>();
        List<FinalIngredient> finalIngredients = new ArrayList<>();
        List<ChangedIngredientResponse> changedIngredients = new ArrayList<>();
        List<NutritionSummary> originalContributions = new ArrayList<>();
        List<NutritionSummary> personalizedContributions = new ArrayList<>();

        for (RecipeIngredient original : ingredients) {
            Ingredient originalIngredient = original.getIngredient();
            IngredientNutrition originalFallback = nutritionMap.get(originalIngredient.getId());
            NutritionSummary originalPerGram = nutritionCalculator.resolvePerGram(
                    original,
                    originalFallback
            );
            originalContributions.add(nutritionCalculator.contributePerGram(
                    originalPerGram,
                    original.getAmount()
            ));

            AppliedSubstitution applied = substitutionsByOriginalId.get(originalIngredient.getId());
            Ingredient finalIngredient = applied == null
                    ? originalIngredient
                    : applied.registered().getSubstitute();
            BigDecimal finalAmount = applied == null ? original.getAmount() : applied.amount();
            NutritionSummary finalNutrition = applied == null
                    ? originalPerGram
                    : nutritionCalculator.toSummary(nutritionMap.get(finalIngredient.getId()));
            if (!finalIngredientIds.add(finalIngredient.getId())) {
                throw new InvalidRecipeSubstitutionException(
                        "대체 결과에 동일한 재료가 중복될 수 없습니다."
                );
            }

            String reason = null;
            if (applied != null) {
                reason = Optional.ofNullable(applied.registered().getReason())
                        .orElse(FALLBACK_MANUAL_CHANGE_REASON);
                changedIngredients.add(new ChangedIngredientResponse(
                        new RecipeIngredientResponse(
                                originalIngredient.getId(),
                                originalIngredient.getTitle(),
                                original.getAmount()
                        ),
                        new RecipeIngredientResponse(
                                finalIngredient.getId(),
                                finalIngredient.getTitle(),
                                finalAmount
                        )
                ));
            }
            finalIngredients.add(new FinalIngredient(
                    finalIngredient,
                    finalAmount,
                    finalNutrition,
                    applied != null,
                    reason
            ));
            personalizedContributions.add(nutritionCalculator.contributePerGram(
                    finalNutrition,
                    finalAmount
            ));
        }

        NutritionSummary originalTotal = nutritionCalculator.sum(originalContributions);
        NutritionSummary personalizedTotal = nutritionCalculator.sum(personalizedContributions);
        return new SubstitutionCalculation(
                recipe,
                originalTotal,
                personalizedTotal,
                nutritionCalculator.changes(originalTotal, personalizedTotal),
                List.copyOf(changedIngredients),
                List.copyOf(finalIngredients)
        );
    }

    private List<PersonalizedIngredientResponse> toPersonalizedIngredientResponses(
            List<FinalIngredient> finalIngredients
    ) {
        return finalIngredients.stream()
                .map(item -> new PersonalizedIngredientResponse(
                        item.ingredient().getId(),
                        item.ingredient().getTitle(),
                        item.amount(),
                        item.changed(),
                        item.changeReason()
                ))
                .toList();
    }

    private Recipe findOwnedRecipe(Long userId, Long recipeId) {
        return recipeRepository.findByIdAndUserId(recipeId, userId)
                .orElseThrow(RecipeNotFoundException::new);
    }

    private HealthProfile findHealthProfile(Long userId) {
        return healthProfileRepository.findByUserId(userId)
                .orElseThrow(HealthProfileNotFoundException::new);
    }

    private Map<Long, IngredientNutrition> loadNutritionMap(Collection<Long> ingredientIds) {
        if (ingredientIds.isEmpty()) {
            return Map.of();
        }
        return ingredientNutritionRepository.findByIngredientIdIn(ingredientIds).stream()
                .collect(Collectors.toMap(
                        nutrition -> nutrition.getIngredient().getId(),
                        nutrition -> nutrition
                ));
    }

    private BigDecimal recommendedAmount(BigDecimal originalAmount, BigDecimal ratio) {
        return originalAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    private RecipeIngredient.NutritionSnapshot snapshot(NutritionSummary nutrition) {
        if (nutrition == null) {
            return null;
        }
        return new RecipeIngredient.NutritionSnapshot(
                nutrition.calories(),
                nutrition.carb(),
                nutrition.protein(),
                nutrition.fat(),
                nutrition.fiber(),
                nutrition.sugar(),
                nutrition.sodium()
        );
    }

    private record AppliedSubstitution(
            IngredientSubstitute registered,
            BigDecimal amount
    ) {
    }

    private record FinalIngredient(
            Ingredient ingredient,
            BigDecimal amount,
            NutritionSummary nutritionPerGram,
            boolean changed,
            String changeReason
    ) {
    }

    private record SubstitutionCalculation(
            Recipe recipe,
            NutritionSummary originalNutrition,
            NutritionSummary personalizedNutrition,
            NutritionSummary nutritionChanges,
            List<ChangedIngredientResponse> changedIngredients,
            List<FinalIngredient> finalIngredients
    ) {
    }
}
