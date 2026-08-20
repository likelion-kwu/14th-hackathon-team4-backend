package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "개인화 레시피 추천 응답")
public record RecipeRecommendationResponse(
        @Schema(description = "추천 레시피 목록 (최신순)")
        List<RecipeRecommendationItemResponse> recommendations
) {
}
