package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "저장된 개인화 레시피 응답")
public record SavedPersonalizedRecipeResponse(
        @Schema(description = "새로 저장된 레시피 ID", example = "52")
        Long recipeId,

        @Schema(description = "원본 레시피 ID", example = "10")
        Long sourceRecipeId,

        @Schema(description = "저장된 레시피 제목", example = "닭가슴살 볶음")
        String title,

        @Schema(description = "최종 레시피 전체 영양 정보")
        NutritionSummary nutrition
) {
}
