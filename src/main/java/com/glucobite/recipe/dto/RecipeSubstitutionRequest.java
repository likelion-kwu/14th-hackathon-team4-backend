package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "복수 재료 대체 요청")
public record RecipeSubstitutionRequest(
        @Schema(description = "현재까지 선택한 전체 대체 항목")
        @NotEmpty
        @Size(max = 100)
        List<@NotNull @Valid IngredientSubstitutionRequest> substitutions
) {
}
