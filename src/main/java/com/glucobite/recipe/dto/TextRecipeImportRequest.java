package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "텍스트 레시피 분석 요청")
public record TextRecipeImportRequest(
        @Schema(
                description = "분석할 레시피 원문",
                example = "재료: 계란 2개, 밥 1공기. 팬에 재료를 넣고 5분간 볶습니다.",
                maxLength = 50_000
        )
        @NotBlank(message = "레시피 텍스트를 입력해 주세요.")
        @Size(max = 50_000, message = "레시피 텍스트는 50,000자 이하여야 합니다.")
        String text
) {
}
