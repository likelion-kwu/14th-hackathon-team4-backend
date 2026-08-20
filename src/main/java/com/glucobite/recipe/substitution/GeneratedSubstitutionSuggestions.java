package com.glucobite.recipe.substitution;

import com.glucobite.recipe.dto.NutritionSummary;

import java.math.BigDecimal;
import java.util.List;

public record GeneratedSubstitutionSuggestions(
        String responseId,
        List<Suggestion> suggestions,
        List<Source> sources
) {
    public record Suggestion(
            String title,
            BigDecimal recommendedAmount,
            String reason,
            String warning,
            NutritionSummary nutritionPerGram
    ) {
    }

    public record Source(String title, String url) {
    }
}
