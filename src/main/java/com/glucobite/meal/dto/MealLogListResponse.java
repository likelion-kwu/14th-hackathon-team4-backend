package com.glucobite.meal.dto;

import java.time.LocalDate;
import java.util.List;

public record MealLogListResponse(
        LocalDate from,
        LocalDate to,
        List<MealLogResponse> meals
) {
}
