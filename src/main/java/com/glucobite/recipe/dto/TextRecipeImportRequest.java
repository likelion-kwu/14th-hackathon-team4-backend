package com.glucobite.recipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextRecipeImportRequest(
        @NotBlank(message = "레시피 텍스트를 입력해 주세요.")
        @Size(max = 50_000, message = "레시피 텍스트는 50,000자 이하여야 합니다.")
        String text
) {
}
