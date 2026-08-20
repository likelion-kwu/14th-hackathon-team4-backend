package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "개인화 레시피 최종 저장 요청")
public record SavePersonalizedRecipeRequest(
        @Schema(description = "새 레시피 제목. 미입력 시 원본 제목을 사용합니다.", example = "닭가슴살 볶음")
        @Size(max = 150)
        String title,

        @Schema(description = "최종 선택한 전체 대체 항목")
        @NotEmpty
        @Size(max = 100)
        List<@NotNull @Valid IngredientSubstitutionRequest> substitutions
) {
}
