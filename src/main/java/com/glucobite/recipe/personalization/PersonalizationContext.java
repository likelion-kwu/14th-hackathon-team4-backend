package com.glucobite.recipe.personalization;

import java.math.BigDecimal;
import java.util.List;

public record PersonalizationContext(
        RecipeData recipe,
        HealthData health,
        List<CatalogIngredient> ingredientCatalog,
        PreviousCandidate previousCandidate
) {
    public record RecipeData(
            String title,
            String description,
            Integer cookingTime,
            List<RecipeIngredientData> ingredients,
            List<String> steps
    ) {
    }

    public record RecipeIngredientData(Long ingredientId, String title, BigDecimal amount) {
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

    public record CatalogIngredient(
            Long ingredientId,
            String title,
            BigDecimal calories,
            BigDecimal carb,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal fiber
    ) {
    }

    public record PreviousCandidate(String title, List<String> ingredients) {
    }
}
