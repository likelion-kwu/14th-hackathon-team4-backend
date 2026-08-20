package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "레시피에 포함된 재료")
public record RecipeIngredientResponse(
        @Schema(description = "재료 ID", example = "1")
        Long ingredientId,

        @Schema(description = "재료명", example = "현미밥")
        String title,

        @Schema(description = "사용량 (1인분 기준 계수)", example = "150.0")
        BigDecimal amount
) {
}
