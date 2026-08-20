package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대체 재료 적용 결과 응답 (Stateless)")
public record ApplySubstituteResponse(
        @Schema(description = "레시피 ID", example = "1")
        Long recipeId,

        @Schema(description = "결과 메시지", example = "대체 재료가 적용되었습니다.")
        String message,

        @Schema(description = "변경된 재료 (원본/대체 정보)")
        ChangedIngredientResponse changedIngredient,

        @Schema(description = "대체 후 전체 영양 정보")
        NutritionSummary nutrition,

        @Schema(description = "기존 대비 전체 영양 변화량")
        NutritionSummary nutritionChanges
) {
}
