package com.glucobite.meal.dto;

import com.glucobite.meal.entity.MealLog;
import com.glucobite.meal.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "식사 기록")
public record MealLogResponse(
        Long mealLogId,
        Long recipeId,
        String title,
        String imageUrl,
        MealType mealType,
        BigDecimal calories,
        BigDecimal carb,
        BigDecimal sugar,
        LocalDateTime eatenAt
) {
    public static MealLogResponse from(MealLog mealLog) {
        return new MealLogResponse(
                mealLog.getId(),
                mealLog.getRecipe() == null ? null : mealLog.getRecipe().getId(),
                mealLog.getTitle(),
                mealLog.getImageUrl(),
                mealLog.getMealType(),
                mealLog.getCalories(),
                mealLog.getCarb(),
                mealLog.getSugar(),
                mealLog.getEatenAt()
        );
    }
}
