package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GeneratePersonalizedRecipeRequest(
        @Schema(
                description = "다른 후보 생성 시 직전 PERSONALIZATION_CANDIDATE Recipe ID",
                example = "12",
                nullable = true
        )
        Long previousCandidateId
) {
}
