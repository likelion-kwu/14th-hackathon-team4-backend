package com.glucobite.recipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record YouTubeRecipeImportRequest(
        @NotBlank(message = "YouTube URL을 입력해 주세요.")
        @Size(max = 500, message = "YouTube URL은 500자 이하여야 합니다.")
        String url
) {
}
