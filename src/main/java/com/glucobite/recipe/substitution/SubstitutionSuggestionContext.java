package com.glucobite.recipe.substitution;

import java.math.BigDecimal;
import java.util.List;

public record SubstitutionSuggestionContext(
        String userInput,
        RecipeData recipe,
        IngredientData originalIngredient,
        HealthData health,
        List<String> excludedSuggestions
) {
    public record RecipeData(
            String title,
            String description,
            Integer cookingTime,
            List<IngredientData> ingredients,
            List<String> steps
    ) {
    }

    public record IngredientData(Long ingredientId, String title, BigDecimal amount) {
    }

    public record HealthData(
            String healthGoal,
            String diabetesStatus,
            Integer dailyCarbsTarget,
            String vegetarianType,
            List<String> allergens,
            String dietaryRestrictionNote
    ) {
    }
}

