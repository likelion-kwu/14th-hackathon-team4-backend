package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "사용자 입력 기반 재료 대체 후보 목록")
public record IngredientAlternativeSuggestionListResponse(
        Long recipeId,
        RecipeIngredientResponse originalIngredient,
        List<IngredientAlternativeSuggestionResponse> suggestions
) {
}
