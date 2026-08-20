package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대체 후보 출처")
public enum IngredientAlternativeSuggestionOrigin {
    REGISTERED,
    AI_WEB_SEARCH
}
