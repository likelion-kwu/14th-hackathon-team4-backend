package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "영양 정보 (총합 또는 변화량). 변화량으로 사용될 때는 음수가 포함될 수 있음")
public record NutritionSummary(
        @Schema(description = "칼로리(kcal)", example = "520.50")
        BigDecimal calories,

        @Schema(description = "탄수화물(g)", example = "72.30")
        BigDecimal carb,

        @Schema(description = "단백질(g)", example = "24.10")
        BigDecimal protein,

        @Schema(description = "지방(g)", example = "12.40")
        BigDecimal fat,

        @Schema(description = "식이섬유(g)", example = "6.10")
        BigDecimal fiber
) {

    public static NutritionSummary zero() {
        return new NutritionSummary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
