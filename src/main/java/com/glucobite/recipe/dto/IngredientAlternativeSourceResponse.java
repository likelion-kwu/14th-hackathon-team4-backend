package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "web search 출처")
public record IngredientAlternativeSourceResponse(
        @Schema(description = "출처 제목", example = "식품 영양정보")
        String title,

        @Schema(description = "클릭 가능한 출처 URL", example = "https://example.com/nutrition")
        String url
) {
}

