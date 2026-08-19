package com.glucobite.recipe.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record RecipePageResponse(
        List<RecipeSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
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
