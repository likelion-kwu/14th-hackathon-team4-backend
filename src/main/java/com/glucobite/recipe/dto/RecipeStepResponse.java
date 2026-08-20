package com.glucobite.recipe.dto;

import com.glucobite.recipe.entity.RecipeStep;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "조리 단계")
public record RecipeStepResponse(
        @Schema(description = "단계 순서", example = "1")
        Integer stepOrder,

        @Schema(description = "단계 설명", example = "재료를 손질해주세요.")
        String description
) {

    public static RecipeStepResponse from(RecipeStep step) {
        return new RecipeStepResponse(step.getStepOrder(), step.getDescription());
    }
}
