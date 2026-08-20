package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "레시피 페이지 응답")
public record RecipePageResponse(
        @ArraySchema(schema = @Schema(implementation = RecipeSummaryResponse.class))
        List<RecipeSummaryResponse> content,

        @Schema(description = "현재 페이지 번호", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "전체 레시피 수", example = "1")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages,

        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext
) {

    public static RecipePageResponse from(Page<RecipeSummaryResponse> result) {
        return new RecipePageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }
}
