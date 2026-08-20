package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "보유 레시피 건강 조건 필터링 응답")
public record RecipeRecommendationResponse(
        @Schema(description = "추천 레시피 목록 (최신순)")
        List<RecipeRecommendationItemResponse> recommendations
) {
}
