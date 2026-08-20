package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "레시피 상세 조회 응답")
public record RecipeDetailResponse(
        @Schema(description = "레시피 ID", example = "1")
        Long recipeId,

        @Schema(description = "제목", example = "닭가슴살 볶음밥")
        String title,

        @Schema(description = "설명", example = "탄수화물과 단백질 균형을 고려한 볶음밥입니다.")
        String description,

        @Schema(description = "조리 시간(분)", example = "20")
        Integer cookingTime,

        @Schema(description = "레시피 전체 영양 정보")
        NutritionSummary nutrition,

        @Schema(description = "재료 목록")
        List<RecipeIngredientResponse> ingredients,

        @Schema(description = "조리 단계 (step_order ASC)")
        List<RecipeStepResponse> steps
) {
}
