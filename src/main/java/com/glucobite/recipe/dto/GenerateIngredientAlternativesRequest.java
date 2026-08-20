package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "사용자 입력 기반 재료 대체 후보 생성 요청")
public record GenerateIngredientAlternativesRequest(
        @Schema(description = "보유 재료 또는 원하는 대체 방향", example = "집에 있는 두부로 바꾸고 싶어")
        @NotBlank
        @Size(max = 300)
        String userInput,

        @Schema(description = "다시 추천하지 않을 이전 AI 후보 ID")
        @Size(max = 10)
        List<@NotNull @Positive Long> excludeSuggestionIds
) {
    public GenerateIngredientAlternativesRequest {
        excludeSuggestionIds = excludeSuggestionIds == null
                ? List.of()
                : List.copyOf(excludeSuggestionIds);
    }
}
