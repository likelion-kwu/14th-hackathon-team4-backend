package com.glucobite.recipe.service;

import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.recipe.dto.NutritionSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * amount 값을 계수로 그대로 사용해 (nutrition × amount) 합산으로 총 영양을 계산한다.
 * INGREDIENT_NUTRITIONS의 값을 1인분·1단위 기준으로 간주한다.
 */
@Component
public class RecipeNutritionCalculator {

    public NutritionSummary contribute(IngredientNutrition nutrition, BigDecimal amount) {
        if (nutrition == null || amount == null) {
            return NutritionSummary.zero();
        }
        return new NutritionSummary(
                multiply(nutrition.getCalories(), amount),
                multiply(nutrition.getCarb(), amount),
                multiply(nutrition.getProtein(), amount),
                multiply(nutrition.getFat(), amount),
                multiply(nutrition.getFiber(), amount),
                multiply(nutrition.getSugar(), amount),
                multiply(nutrition.getSodium(), amount)
        );
    }

    public NutritionSummary sum(Collection<NutritionSummary> summaries) {
        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal fiber = BigDecimal.ZERO;
        BigDecimal sugar = BigDecimal.ZERO;
        BigDecimal sodium = BigDecimal.ZERO;
        for (NutritionSummary summary : summaries) {
            if (summary == null) {
                continue;
            }
            calories = calories.add(nullToZero(summary.calories()));
            carb = carb.add(nullToZero(summary.carb()));
            protein = protein.add(nullToZero(summary.protein()));
            fat = fat.add(nullToZero(summary.fat()));
            fiber = fiber.add(nullToZero(summary.fiber()));
            sugar = sugar.add(nullToZero(summary.sugar()));
            sodium = sodium.add(nullToZero(summary.sodium()));
        }
        return new NutritionSummary(calories, carb, protein, fat, fiber, sugar, sodium);
    }

    /**
     * personalized - base 를 각 필드별로 계산해 변화량을 NutritionSummary로 반환한다.
     * 음수가 포함될 수 있다.
     */
    public NutritionSummary changes(NutritionSummary base, NutritionSummary personalized) {
        return new NutritionSummary(
                subtract(personalized.calories(), base.calories()),
                subtract(personalized.carb(), base.carb()),
                subtract(personalized.protein(), base.protein()),
                subtract(personalized.fat(), base.fat()),
                subtract(personalized.fiber(), base.fiber()),
                subtract(personalized.sugar(), base.sugar()),
                subtract(personalized.sodium(), base.sodium())
        );
    }

    private BigDecimal multiply(BigDecimal value, BigDecimal amount) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.multiply(amount);
    }

    private BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return nullToZero(a).subtract(nullToZero(b));
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
