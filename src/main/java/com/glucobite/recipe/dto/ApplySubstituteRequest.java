package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "대체 재료 적용 요청")
public record ApplySubstituteRequest(
        @Schema(description = "레시피의 기존 재료 ID", example = "5")
        @NotNull @Positive Long originalIngredientId,

        @Schema(description = "대체할 재료 ID", example = "6")
        @NotNull @Positive Long substituteIngredientId,

        @Schema(description = "대체 재료 사용량", example = "150.0")
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount
) {
}
