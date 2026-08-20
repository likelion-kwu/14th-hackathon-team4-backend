package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "대체 가능 재료 항목")
public record IngredientAlternativeResponse(
        @Schema(description = "대체 재료 ID", example = "6")
        Long ingredientId,

        @Schema(description = "대체 재료명", example = "현미밥")
        String title,

        @Schema(description = "권장 사용량 (원본 amount × ratio)", example = "150.0")
        BigDecimal recommendedAmount,

        @Schema(description = "원본 재료 대비 이 재료 하나의 영양 변화량")
        NutritionSummary nutritionChanges,

        @Schema(description = "추천 사유", example = "탄수화물 감소와 식이섬유 증가에 도움이 됩니다.")
        String recommendationReason
) {
}
