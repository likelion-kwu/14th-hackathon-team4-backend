package com.glucobite.recipe.service;

import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.dto.RecipeDetailResponse;
import com.glucobite.recipe.dto.RecipeIngredientResponse;
import com.glucobite.recipe.dto.RecipePageResponse;
import com.glucobite.recipe.dto.RecipeRecommendationItemResponse;
import com.glucobite.recipe.dto.RecipeRecommendationResponse;
import com.glucobite.recipe.dto.RecipeStepListResponse;
import com.glucobite.recipe.dto.RecipeStepResponse;
import com.glucobite.recipe.dto.RecipeSummaryResponse;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.exception.RecipeNotFoundException;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private static final int RECOMMENDATION_LIMIT = 20;
    private static final String RECOMMENDATION_REASON =
            "회원님의 알레르기와 하루 탄수화물 목표를 반영한 추천입니다.";

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final RecipeRepository recipeRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientNutritionRepository ingredientNutritionRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final RecipeNutritionCalculator nutritionCalculator;
    private final DietaryRestrictionPolicy dietaryRestrictionPolicy;

    public RecipeService(
            RecipeRepository recipeRepository,
            RecipeStepRepository recipeStepRepository,
            RecipeIngredientRepository recipeIngredientRepository,
            IngredientNutritionRepository ingredientNutritionRepository,
            HealthProfileRepository healthProfileRepository,
            RecipeNutritionCalculator nutritionCalculator,
            DietaryRestrictionPolicy dietaryRestrictionPolicy
    ) {
        this.recipeRepository = recipeRepository;
        this.recipeStepRepository = recipeStepRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientNutritionRepository = ingredientNutritionRepository;
        this.healthProfileRepository = healthProfileRepository;
        this.nutritionCalculator = nutritionCalculator;
        this.dietaryRestrictionPolicy = dietaryRestrictionPolicy;
    }

    @Transactional(readOnly = true)
    public RecipePageResponse findRecipes(
            Long userId,
            Boolean completed,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<Recipe> recipes = completed == null
                ? recipeRepository.findByUserId(userId, pageable)
                : recipeRepository.findByUserIdAndCompleted(userId, completed, pageable);

        return RecipePageResponse.from(recipes.map(RecipeSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public RecipeDetailResponse getRecipeDetail(Long userId, Long recipeId) {
        Recipe recipe = findOwnedRecipe(userId, recipeId);
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByRecipeId(recipeId);
        Map<Long, IngredientNutrition> nutritionMap = loadNutritionMap(ingredients);
        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getCookingTime(),
                totalNutrition(ingredients, nutritionMap),
                toIngredientResponses(ingredients),
                loadStepResponses(recipeId)
        );
    }

    @Transactional(readOnly = true)
    public RecipeStepListResponse getRecipeSteps(Long userId, Long recipeId) {
        Recipe recipe = findOwnedRecipe(userId, recipeId);
        List<RecipeStepResponse> steps = loadStepResponses(recipeId);
        return new RecipeStepListResponse(recipe.getId(), recipe.getTitle(), steps.size(), steps);
    }

    @Transactional(readOnly = true)
    public RecipeRecommendationResponse getRecommendations(Long userId) {
        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(HealthProfileNotFoundException::new);
        Set<String> restrictedTerms = dietaryRestrictionPolicy.restrictedTerms(profile);

        List<Recipe> recipes = recipeRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId);
        List<Long> recipeIds = recipes.stream().map(Recipe::getId).toList();
        Map<Long, List<RecipeIngredient>> ingredientsByRecipe = loadIngredientsByRecipe(recipeIds);
        Map<Long, IngredientNutrition> nutritionMap = loadNutritionMap(
                ingredientsByRecipe.values().stream().flatMap(Collection::stream).toList()
        );

        List<RecipeRecommendationItemResponse> recommendations = new ArrayList<>();
        for (Recipe recipe : recipes) {
            List<RecipeIngredient> ingredients = ingredientsByRecipe.getOrDefault(recipe.getId(), List.of());
            if (containsRestrictedIngredient(ingredients, restrictedTerms)) {
                continue;
            }
            NutritionSummary total = totalNutrition(ingredients, nutritionMap);
            if (exceedsDailyCarbsTarget(total, profile.getDailyCarbsTarget())) {
                continue;
            }
            recommendations.add(new RecipeRecommendationItemResponse(
                    recipe.getId(),
                    recipe.getTitle(),
                    recipe.getDescription(),
                    recipe.getCookingTime(),
                    total,
                    RECOMMENDATION_REASON
            ));
            if (recommendations.size() == RECOMMENDATION_LIMIT) {
                break;
            }
        }
        return new RecipeRecommendationResponse(recommendations);
    }

    private Recipe findOwnedRecipe(Long userId, Long recipeId) {
        return recipeRepository.findByIdAndUserId(recipeId, userId)
                .orElseThrow(RecipeNotFoundException::new);
    }

    private List<RecipeStepResponse> loadStepResponses(Long recipeId) {
        return recipeStepRepository.findByRecipeIdOrderByStepOrderAsc(recipeId).stream()
                .map(RecipeStepResponse::from)
                .toList();
    }

    private Map<Long, IngredientNutrition> loadNutritionMap(List<RecipeIngredient> ingredients) {
        Set<Long> ingredientIds = ingredients.stream()
                .map(recipeIngredient -> recipeIngredient.getIngredient().getId())
                .collect(Collectors.toSet());
        if (ingredientIds.isEmpty()) {
            return Map.of();
        }
        return ingredientNutritionRepository.findByIngredientIdIn(ingredientIds).stream()
                .collect(Collectors.toMap(
                        nutrition -> nutrition.getIngredient().getId(),
                        nutrition -> nutrition
                ));
    }

    private List<RecipeIngredientResponse> toIngredientResponses(List<RecipeIngredient> ingredients) {
        return ingredients.stream()
                .map(recipeIngredient -> new RecipeIngredientResponse(
                        recipeIngredient.getIngredient().getId(),
                        recipeIngredient.getIngredient().getTitle(),
                        recipeIngredient.getAmount()
                ))
                .toList();
    }

    private NutritionSummary totalNutrition(
            List<RecipeIngredient> ingredients,
            Map<Long, IngredientNutrition> nutritionMap
    ) {
        return nutritionCalculator.sum(ingredients.stream()
                .map(recipeIngredient -> nutritionCalculator.contribute(
                        nutritionMap.get(recipeIngredient.getIngredient().getId()),
                        recipeIngredient.getAmount()
                ))
                .toList());
    }

    private Map<Long, List<RecipeIngredient>> loadIngredientsByRecipe(List<Long> recipeIds) {
        if (recipeIds.isEmpty()) {
            return Map.of();
        }
        return recipeIngredientRepository.findByRecipeIdIn(recipeIds).stream()
                .collect(Collectors.groupingBy(
                        ingredient -> ingredient.getRecipe().getId()
                ));
    }

    private boolean containsRestrictedIngredient(
            List<RecipeIngredient> ingredients,
            Set<String> restrictedTerms
    ) {
        for (RecipeIngredient recipeIngredient : ingredients) {
            if (dietaryRestrictionPolicy.isRestricted(
                    recipeIngredient.getIngredient(), restrictedTerms
            )) {
                return true;
            }
        }
        return false;
    }

    private boolean exceedsDailyCarbsTarget(NutritionSummary nutrition, Integer dailyCarbsTarget) {
        BigDecimal carb = nutrition.carb();
        return carb != null
                && dailyCarbsTarget != null
                && carb.compareTo(BigDecimal.valueOf(dailyCarbsTarget)) > 0;
    }
}
