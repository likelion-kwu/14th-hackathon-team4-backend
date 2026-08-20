package com.glucobite.recipe.personalization;

import java.math.BigDecimal;
import java.util.List;

public record GeneratedPersonalization(
        String responseId,
        String label,
        String title,
        String description,
        Integer cookingTime,
        String reason,
        List<IngredientAmount> ingredients,
        List<String> steps
) {
    public record IngredientAmount(Long ingredientId, BigDecimal amount) {
    }
}
