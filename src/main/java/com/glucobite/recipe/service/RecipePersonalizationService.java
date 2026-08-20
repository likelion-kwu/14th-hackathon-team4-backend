package com.glucobite.recipe.service;

import com.glucobite.health.entity.Allergen;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.entity.VegetarianType;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.entity.IngredientSubstitute;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.ingredient.repository.IngredientSubstituteRepository;
import com.glucobite.recipe.dto.ApplySubstituteRequest;
import com.glucobite.recipe.dto.ApplySubstituteResponse;
import com.glucobite.recipe.dto.ChangedIngredientResponse;
import com.glucobite.recipe.dto.IngredientAlternativeListResponse;
import com.glucobite.recipe.dto.IngredientAlternativeResponse;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.dto.PersonalizedIngredientResponse;
import com.glucobite.recipe.dto.PersonalizedRecipeDetailResponse;
import com.glucobite.recipe.dto.RecipeIngredientResponse;
import com.glucobite.recipe.dto.RecipeStepResponse;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.exception.IngredientNotFoundException;
import com.glucobite.recipe.exception.InvalidSubstituteIngredientException;
import com.glucobite.recipe.exception.RecipeIngredientNotFoundException;
import com.glucobite.recipe.exception.RecipeNotFoundException;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecipePersonalizationService {

    private static final String APPLY_SUCCESS_MESSAGE = "대체 재료가 적용되었습니다.";
    private static final String FALLBACK_AUTO_CHANGE_REASON =
            "회원님의 알레르기 정보를 반영해 자동 대체되었습니다.";

    private final RecipeRepository recipeRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientNutritionRepository ingredientNutritionRepository;
    private final IngredientSubstituteRepository ingredientSubstituteRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final RecipeNutritionCalculator nutritionCalculator;

    public RecipePersonalizationService(
            RecipeRepository recipeRepository,
            RecipeStepRepository recipeStepRepository,
            RecipeIngredientRepository recipeIngredientRepository,
            IngredientNutritionRepository ingredientNutritionRepository,
            IngredientSubstituteRepository ingredientSubstituteRepository,
            HealthProfileRepository healthProfileRepository,
            RecipeNutritionCalculator nutritionCalculator
    ) {
        this.recipeRepository = recipeRepository;
        this.recipeStepRepository = recipeStepRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientNutritionRepository = ingredientNutritionRepository;
        this.ingredientSubstituteRepository = ingredientSubstituteRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.nutritionCalculator = nutritionCalculator;
    }

    @Transactional(readOnly = true)
    public PersonalizedRecipeDetailResponse getPersonalizedDetail(Long userId, Long recipeId) {
        Recipe recipe = findOwnedRecipe(userId, recipeId);
        HealthProfile profile = findHealthProfile(userId);
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByRecipeId(recipeId);
        List<RecipeStepResponse> steps = recipeStepRepository.findByRecipeIdOrderByStepOrderAsc(recipeId)
                .stream()
                .map(RecipeStepResponse::from)
                .toList();

        ProfileRestrictions restrictions = restrictionsFor(profile);
        Map<Long, IngredientSubstitute> autoSubstitutes = resolveAutoSubstitutes(
                ingredients,
                restrictions
        );
        Set<Long> nutritionIds = new HashSet<>();
        for (RecipeIngredient ingredient : ingredients) {
            nutritionIds.add(ingredient.getIngredient().getId());
        }
        for (IngredientSubstitute substitute : autoSubstitutes.values()) {
            nutritionIds.add(substitute.getSubstitute().getId());
        }
        Map<Long, IngredientNutrition> nutritionMap = loadNutritionMap(nutritionIds);

        List<PersonalizedIngredientResponse> ingredientResponses = new ArrayList<>();
        List<NutritionSummary> originalContributions = new ArrayList<>();
        List<NutritionSummary> personalizedContributions = new ArrayList<>();

        for (RecipeIngredient ingredient : ingredients) {
            Ingredient source = ingredient.getIngredient();
            BigDecimal originalAmount = ingredient.getAmount();
            IngredientNutrition originalNutrition = nutritionMap.get(source.getId());
            NutritionSummary originalContribution = nutritionCalculator.contribute(originalNutrition, originalAmount);
            originalContributions.add(originalContribution);

            IngredientSubstitute substitute = autoSubstitutes.get(source.getId());
            if (substitute == null) {
                ingredientResponses.add(new PersonalizedIngredientResponse(
                        source.getId(),
                        source.getTitle(),
                        originalAmount,
                        false,
                        null
                ));
                personalizedContributions.add(originalContribution);
            } else {
                Ingredient substituteIngredient = substitute.getSubstitute();
                BigDecimal newAmount = originalAmount.multiply(substitute.getRatio());
                IngredientNutrition substituteNutrition = nutritionMap.get(substituteIngredient.getId());
                NutritionSummary substituteContribution = nutritionCalculator
                        .contribute(substituteNutrition, newAmount);
                ingredientResponses.add(new PersonalizedIngredientResponse(
                        substituteIngredient.getId(),
                        substituteIngredient.getTitle(),
                        newAmount,
                        true,
                        Optional.ofNullable(substitute.getReason()).orElse(FALLBACK_AUTO_CHANGE_REASON)
                ));
                personalizedContributions.add(substituteContribution);
            }
        }

        NutritionSummary originalTotal = nutritionCalculator.sum(originalContributions);
        NutritionSummary personalizedTotal = nutritionCalculator.sum(personalizedContributions);
        NutritionSummary changes = nutritionCalculator.changes(originalTotal, personalizedTotal);

        return new PersonalizedRecipeDetailResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getCookingTime(),
                originalTotal,
                personalizedTotal,
                changes,
                ingredientResponses,
                steps
        );
    }

    @Transactional(readOnly = true)
    public IngredientAlternativeListResponse getAlternatives(
            Long userId,
            Long recipeId,
            Long ingredientId
    ) {
        findOwnedRecipe(userId, recipeId);
        ProfileRestrictions restrictions = restrictionsFor(findHealthProfile(userId));
        RecipeIngredient recipeIngredient = recipeIngredientRepository
                .findByRecipeIdAndIngredientId(recipeId, ingredientId)
                .orElseThrow(IngredientNotFoundException::new);
        Ingredient original = recipeIngredient.getIngredient();
        BigDecimal originalAmount = recipeIngredient.getAmount();
        IngredientNutrition originalNutrition = ingredientNutritionRepository
                .findByIngredientId(ingredientId)
                .orElse(null);
        NutritionSummary originalContribution = nutritionCalculator.contribute(originalNutrition, originalAmount);

        List<IngredientSubstitute> substitutes = ingredientSubstituteRepository
                .findByIngredientIdOrderByIdAsc(ingredientId).stream()
                .filter(substitute -> !isRestricted(substitute.getSubstitute(), restrictions))
                .toList();
        Set<Long> substituteIngredientIds = substitutes.stream()
                .map(substitute -> substitute.getSubstitute().getId())
                .collect(Collectors.toSet());
        Map<Long, IngredientNutrition> substituteNutritions = loadNutritionMap(substituteIngredientIds);

        List<IngredientAlternativeResponse> alternatives = new ArrayList<>();
        for (IngredientSubstitute substitute : substitutes) {
            Ingredient substituteIngredient = substitute.getSubstitute();
            BigDecimal recommendedAmount = originalAmount.multiply(substitute.getRatio());
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
    public ApplySubstituteResponse applySubstitute(
            Long userId,
            Long recipeId,
            ApplySubstituteRequest request
    ) {
        findOwnedRecipe(userId, recipeId);
        ProfileRestrictions restrictions = restrictionsFor(findHealthProfile(userId));
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByRecipeId(recipeId);
        RecipeIngredient target = ingredients.stream()
                .filter(ingredient -> ingredient.getIngredient().getId().equals(request.originalIngredientId()))
                .findFirst()
                .orElseThrow(RecipeIngredientNotFoundException::new);

        IngredientSubstitute registered = ingredientSubstituteRepository
                .findByIngredientIdAndSubstituteId(request.originalIngredientId(), request.substituteIngredientId())
                .orElseThrow(InvalidSubstituteIngredientException::new);
        if (isRestricted(registered.getSubstitute(), restrictions)) {
            throw new InvalidSubstituteIngredientException();
        }

        Set<Long> lookupIds = new HashSet<>();
        for (RecipeIngredient ingredient : ingredients) {
            lookupIds.add(ingredient.getIngredient().getId());
        }
        lookupIds.add(registered.getSubstitute().getId());
        Map<Long, IngredientNutrition> nutritionMap = loadNutritionMap(lookupIds);

        NutritionSummary originalTotal = totalNutrition(ingredients, nutritionMap);
        BigDecimal appliedAmount = request.amount();
        Ingredient substituteIngredient = registered.getSubstitute();
        IngredientNutrition substituteNutrition = nutritionMap.get(substituteIngredient.getId());
        NutritionSummary substituteContribution = nutritionCalculator
                .contribute(substituteNutrition, appliedAmount);
        NutritionSummary originalContribution = nutritionCalculator
                .contribute(nutritionMap.get(target.getIngredient().getId()), target.getAmount());
        NutritionSummary personalizedTotal = nutritionCalculator.sum(List.of(
                originalTotal,
                nutritionCalculator.changes(originalContribution, substituteContribution)
        ));
        NutritionSummary changes = nutritionCalculator.changes(originalTotal, personalizedTotal);

        Ingredient originalIngredient = target.getIngredient();
        ChangedIngredientResponse changed = new ChangedIngredientResponse(
                new RecipeIngredientResponse(
                        originalIngredient.getId(),
                        originalIngredient.getTitle(),
                        target.getAmount()
                ),
                new RecipeIngredientResponse(
                        substituteIngredient.getId(),
                        substituteIngredient.getTitle(),
                        appliedAmount
                )
        );

        return new ApplySubstituteResponse(
                recipeId,
                APPLY_SUCCESS_MESSAGE,
                changed,
                personalizedTotal,
                changes
        );
    }

    private Map<Long, IngredientSubstitute> resolveAutoSubstitutes(
            List<RecipeIngredient> ingredients,
            ProfileRestrictions restrictions
    ) {
        Map<Long, IngredientSubstitute> substitutes = new java.util.LinkedHashMap<>();
        if (restrictions.restrictedTerms().isEmpty()) {
            return substitutes;
        }
        for (RecipeIngredient ingredient : ingredients) {
            Ingredient source = ingredient.getIngredient();
            if (!isRestricted(source, restrictions)) {
                continue;
            }
            ingredientSubstituteRepository.findByIngredientIdOrderByIdAsc(source.getId()).stream()
                    .filter(substitute -> !isRestricted(substitute.getSubstitute(), restrictions))
                    .findFirst()
                    .ifPresent(substitute -> substitutes.put(source.getId(), substitute));
        }
        return substitutes;
    }

    private Recipe findOwnedRecipe(Long userId, Long recipeId) {
        return recipeRepository.findByIdAndUserId(recipeId, userId)
                .orElseThrow(RecipeNotFoundException::new);
    }

    private HealthProfile findHealthProfile(Long userId) {
        return healthProfileRepository.findByUserId(userId)
                .orElseThrow(HealthProfileNotFoundException::new);
    }

    private ProfileRestrictions restrictionsFor(HealthProfile profile) {
        Set<String> restrictedTerms = toAllergenNames(profile.getAllergens());
        restrictedTerms.addAll(vegetarianRestrictedTerms(profile.getVegetarianType()));
        return new ProfileRestrictions(Set.copyOf(restrictedTerms));
    }

    private Set<String> vegetarianRestrictedTerms(VegetarianType vegetarianType) {
        if (vegetarianType == null || vegetarianType == VegetarianType.NONE) {
            return Set.of();
        }
        Set<String> meatAndSeafood = Set.of(
                "돼지", "소고기", "쇠고기", "닭", "오리", "양고기",
                "생선", "고등어", "연어", "참치", "새우", "게", "오징어", "조개"
        );
        if (vegetarianType == VegetarianType.PESCATARIAN) {
            return Set.of("돼지", "소고기", "쇠고기", "닭", "오리", "양고기");
        }
        Set<String> restricted = new HashSet<>(meatAndSeafood);
        if (vegetarianType == VegetarianType.VEGAN || vegetarianType == VegetarianType.OVO) {
            restricted.addAll(Set.of("우유", "치즈", "버터", "요거트"));
        }
        if (vegetarianType == VegetarianType.VEGAN || vegetarianType == VegetarianType.LACTO) {
            restricted.addAll(Set.of("달걀", "계란", "난류"));
        }
        if (vegetarianType == VegetarianType.VEGAN) {
            restricted.add("꿀");
        }
        return restricted;
    }

    private boolean isRestricted(Ingredient ingredient, ProfileRestrictions restrictions) {
        String title = ingredient.getTitle();
        return restrictions.restrictedTerms().stream().anyMatch(title::contains);
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

    private NutritionSummary totalNutrition(
            List<RecipeIngredient> ingredients,
            Map<Long, IngredientNutrition> nutritionMap
    ) {
        List<NutritionSummary> contributions = new ArrayList<>();
        for (RecipeIngredient ingredient : ingredients) {
            IngredientNutrition nutrition = nutritionMap.get(ingredient.getIngredient().getId());
            contributions.add(nutritionCalculator.contribute(nutrition, ingredient.getAmount()));
        }
        return nutritionCalculator.sum(contributions);
    }

    private Set<String> toAllergenNames(Set<Allergen> allergens) {
        Set<String> names = new HashSet<>();
        for (Allergen allergen : allergens) {
            names.add(allergen.getName());
        }
        return names;
    }

    private record ProfileRestrictions(Set<String> restrictedTerms) {
    }
}
