package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "대체 가능 재료 목록 응답")
public record IngredientAlternativeListResponse(
        @Schema(description = "대상 레시피 ID", example = "1")
        Long recipeId,

        @Schema(description = "원본 재료 정보")
        RecipeIngredientResponse originalIngredient,

        @Schema(description = "대체 가능 재료 후보 목록")
        List<IngredientAlternativeResponse> alternatives
) {
}
