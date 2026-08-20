package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "적용된 대체 재료 정보")
public record ChangedIngredientResponse(
        @Schema(description = "기존 재료 정보")
        RecipeIngredientResponse originalIngredient,

        @Schema(description = "대체 재료 정보 (권장 사용량 반영)")
        RecipeIngredientResponse substituteIngredient
) {
}
