package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "재료 대체 항목")
public record IngredientSubstitutionRequest(
        @Schema(description = "원본 레시피에 포함된 재료 ID", example = "5")
        @NotNull @Positive Long originalIngredientId,

        @Schema(description = "대체할 등록 재료 ID", example = "6")
        @NotNull @Positive Long substituteIngredientId,

        @Schema(description = "대체 재료 사용량", example = "150.0")
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal amount
) {
}
