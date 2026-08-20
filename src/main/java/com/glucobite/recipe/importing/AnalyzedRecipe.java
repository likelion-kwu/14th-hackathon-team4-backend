package com.glucobite.recipe.importing;

import com.glucobite.recipe.dto.NutritionSummary;

import java.math.BigDecimal;
import java.util.List;

public record AnalyzedRecipe(
        String title,
        String description,
        Integer cookingTime,
        List<IngredientData> ingredients,
        List<String> steps
) {

    public record IngredientData(
            String title,
            BigDecimal amountGrams,
            NutritionSummary nutritionPerGram
    ) {
    }
}
