package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개인화 추천 레시피 항목")
public record RecipeRecommendationItemResponse(
        @Schema(description = "레시피 ID", example = "10")
        Long recipeId,

        @Schema(description = "제목", example = "저탄수 닭가슴살 볶음밥")
        String title,

        @Schema(description = "설명")
        String description,

        @Schema(description = "조리 시간(분)", example = "20")
        Integer cookingTime,

        @Schema(description = "레시피 전체 영양 정보")
        NutritionSummary nutrition,

        @Schema(description = "추천 사유")
        String recommendationReason
) {
}
