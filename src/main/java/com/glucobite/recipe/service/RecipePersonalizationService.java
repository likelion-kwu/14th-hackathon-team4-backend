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
import com.glucobite.recipe.dto.ChangedIngredientResponse;
import com.glucobite.recipe.dto.IngredientSubstitutionRequest;
import com.glucobite.recipe.dto.IngredientAlternativeListResponse;
import com.glucobite.recipe.dto.IngredientAlternativeResponse;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.dto.PersonalizedIngredientResponse;
import com.glucobite.recipe.dto.PersonalizedRecipeDetailResponse;
import com.glucobite.recipe.dto.RecipeIngredientResponse;
import com.glucobite.recipe.dto.RecipeStepResponse;
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

    private static final String FALLBACK_AUTO_CHANGE_REASON =
            "회원님의 알레르기 정보를 반영해 자동 대체되었습니다.";
    private static final String FALLBACK_MANUAL_CHANGE_REASON =
            "선택한 대체 재료가 적용되었습니다.";

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
        Recipe saved = new Recipe(
                source.getUser(),
                title,
                source.getDescription(),
                source.getCookingTime(),
                source.getImportType(),
                calculation.personalizedNutrition().calories().setScale(2, RoundingMode.HALF_UP)
        );
        saved.complete();
        recipeRepository.save(saved);

        List<RecipeIngredient> savedIngredients = calculation.finalIngredients().stream()
                .map(item -> new RecipeIngredient(saved, item.ingredient(), item.amount()))
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
        ProfileRestrictions restrictions = restrictionsFor(findHealthProfile(userId));
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
            if (isRestricted(registered.getSubstitute(), restrictions)) {
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
            originalContributions.add(nutritionCalculator.contribute(
                    nutritionMap.get(originalIngredient.getId()),
                    original.getAmount()
            ));

            AppliedSubstitution applied = substitutionsByOriginalId.get(originalIngredient.getId());
            Ingredient finalIngredient = applied == null
                    ? originalIngredient
                    : applied.registered().getSubstitute();
            BigDecimal finalAmount = applied == null ? original.getAmount() : applied.amount();
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
                    applied != null,
                    reason
            ));
            personalizedContributions.add(nutritionCalculator.contribute(
                    nutritionMap.get(finalIngredient.getId()),
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

    private Set<String> toAllergenNames(Set<Allergen> allergens) {
        Set<String> names = new HashSet<>();
        for (Allergen allergen : allergens) {
            names.add(allergen.getName());
        }
        return names;
    }

    private record ProfileRestrictions(Set<String> restrictedTerms) {
    }

    private record AppliedSubstitution(
            IngredientSubstitute registered,
            BigDecimal amount
    ) {
    }

    private record FinalIngredient(
            Ingredient ingredient,
            BigDecimal amount,
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
