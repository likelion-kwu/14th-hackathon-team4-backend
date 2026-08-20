package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "조리 단계 목록 응답")
public record RecipeStepListResponse(
        @Schema(description = "레시피 ID", example = "1")
        Long recipeId,

        @Schema(description = "레시피 제목", example = "닭가슴살 볶음밥")
        String title,

        @Schema(description = "총 단계 수", example = "3")
        Integer totalSteps,

        @Schema(description = "step_order 오름차순으로 정렬된 조리 단계")
        List<RecipeStepResponse> steps
) {
}
