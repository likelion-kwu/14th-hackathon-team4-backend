package com.glucobite.recipe.dto;

import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeImportType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecipeSummaryResponse(
        Long recipeId,
        String title,
        Integer cookingTime,
        BigDecimal totalCalories,
        RecipeImportType importType,
        boolean completed,
        LocalDateTime createdAt
) {

    public static RecipeSummaryResponse from(Recipe recipe) {
        return new RecipeSummaryResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getCookingTime(),
                recipe.getTotalCalories(),
                recipe.getImportType(),
                recipe.isCompleted(),
                recipe.getCreatedAt()
        );
    }
}
