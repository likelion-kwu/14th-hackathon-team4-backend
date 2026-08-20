package com.glucobite.dashboard.dto;

import com.glucobite.meal.dto.MealLogResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TodaySummaryResponse(
        LocalDate date,
        int mealCount,
        BigDecimal totalCalories,
        BigDecimal totalCarb,
        BigDecimal totalSugar,
        Integer dailyCarbsTarget,
        BigDecimal carbProgressPercent,
        int glucoseRecordCount,
        BigDecimal averageGlucose,
        BigDecimal latestGlucose,
        LocalDateTime latestGlucoseMeasuredAt,
        List<MealLogResponse> meals
) {
}
