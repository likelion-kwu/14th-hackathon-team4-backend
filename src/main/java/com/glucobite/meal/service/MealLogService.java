package com.glucobite.meal.service;

import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.meal.dto.CreateMealLogRequest;
import com.glucobite.meal.dto.MealLogListResponse;
import com.glucobite.meal.dto.MealLogResponse;
import com.glucobite.meal.entity.MealLog;
import com.glucobite.meal.repository.MealLogRepository;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.exception.RecipeNotFoundException;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.service.RecipeNutritionCalculator;
import com.glucobite.tracking.service.TrackingDateRange;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientNutritionRepository nutritionRepository;
    private final RecipeNutritionCalculator nutritionCalculator;

    public MealLogService(
            MealLogRepository mealLogRepository,
            UserRepository userRepository,
            RecipeRepository recipeRepository,
            RecipeIngredientRepository recipeIngredientRepository,
            IngredientNutritionRepository nutritionRepository,
            RecipeNutritionCalculator nutritionCalculator
    ) {
        this.mealLogRepository = mealLogRepository;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.nutritionRepository = nutritionRepository;
        this.nutritionCalculator = nutritionCalculator;
    }

    @Transactional
    public MealLogResponse create(Long userId, CreateMealLogRequest request) {
        User user = userRepository.getReferenceById(userId);
        Recipe recipe = request.recipeId() == null
                ? null
                : recipeRepository.findByIdAndUserId(request.recipeId(), userId)
                .orElseThrow(RecipeNotFoundException::new);

        NutritionValues nutrition = recipe == null
                ? new NutritionValues(request.calories(), request.carb(), request.sugar())
                : nutritionOf(recipe);
        String title = recipe == null ? request.title().strip() : recipe.getTitle();
        String imageUrl = request.imageUrl() != null
                ? request.imageUrl()
                : recipe == null ? null : recipe.getImageUrl();

        return MealLogResponse.from(mealLogRepository.save(new MealLog(
                user,
                recipe,
                title,
                imageUrl,
                request.mealType(),
                nutrition.calories(),
                nutrition.carb(),
                nutrition.sugar(),
                request.eatenAt()
        )));
    }

    @Transactional(readOnly = true)
    public MealLogListResponse get(Long userId, LocalDate from, LocalDate to) {
        TrackingDateRange range = TrackingDateRange.resolve(from, to);
        List<MealLogResponse> meals = mealLogRepository
                .findByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtDescIdDesc(
                        userId, range.startInclusive(), range.endExclusive()
                ).stream()
                .map(MealLogResponse::from)
                .toList();
        return new MealLogListResponse(range.from(), range.to(), meals);
    }

    private NutritionValues nutritionOf(Recipe recipe) {
        List<RecipeIngredient> ingredients = recipeIngredientRepository.findByRecipeId(recipe.getId());
        Map<Long, IngredientNutrition> fallbackByIngredient = nutritionRepository
                .findByIngredientIdIn(ingredients.stream()
                        .map(item -> item.getIngredient().getId())
                        .toList())
                .stream()
                .collect(Collectors.toMap(
                        nutrition -> nutrition.getIngredient().getId(),
                        Function.identity()
                ));
        NutritionSummary total = nutritionCalculator.sum(ingredients.stream()
                .map(item -> nutritionCalculator.contribute(
                        item,
                        fallbackByIngredient.get(item.getIngredient().getId())
                ))
                .toList());
        BigDecimal calories = total.calories().signum() == 0 && recipe.getTotalCalories() != null
                ? recipe.getTotalCalories()
                : total.calories();
        return new NutritionValues(calories, total.carb(), total.sugar());
    }

    private record NutritionValues(BigDecimal calories, BigDecimal carb, BigDecimal sugar) {
    }
}
