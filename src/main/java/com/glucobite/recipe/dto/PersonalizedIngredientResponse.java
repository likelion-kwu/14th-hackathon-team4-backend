package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "개인화 레시피 재료 항목")
public record PersonalizedIngredientResponse(
        @Schema(description = "재료 ID", example = "1")
        Long ingredientId,

        @Schema(description = "재료명", example = "현미밥")
        String title,

        @Schema(description = "사용량", example = "120.0")
        BigDecimal amount,

        @Schema(description = "원본 대비 변경 여부", example = "false")
        boolean changed,

        @Schema(description = "변경 사유. 변경되지 않았으면 null", example = "탄수화물 섭취량 조절을 위해 변경했습니다.")
        String changeReason
) {
}
