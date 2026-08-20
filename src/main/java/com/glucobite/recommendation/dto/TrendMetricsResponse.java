package com.glucobite.recommendation.dto;

import java.math.BigDecimal;

public record TrendMetricsResponse(
        int mealCount,
        int glucoseRecordCount,
        int daysWithMeals,
        int carbTargetExceededDays,
        BigDecimal averageDailyCarb,
        BigDecimal averageSugarPerMeal,
        BigDecimal averageGlucose,
        BigDecimal postMealGlucoseChangePercent
) {
}
