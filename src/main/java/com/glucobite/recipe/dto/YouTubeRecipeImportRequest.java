package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "YouTube 레시피 분석 요청")
public record YouTubeRecipeImportRequest(
        @Schema(
                description = "분석할 공개 YouTube 영상 URL",
                example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                maxLength = 500
        )
        @NotBlank(message = "YouTube URL을 입력해 주세요.")
        @Size(max = 500, message = "YouTube URL은 500자 이하여야 합니다.")
        String url
) {
}
