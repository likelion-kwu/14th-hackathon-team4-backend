package com.glucobite.recipe.dto;

import com.glucobite.recipe.entity.RecipeImportType;
import com.glucobite.recipe.entity.RecipeType;

import java.util.List;

public record ImportedRecipeResponse(
        Long recipeId,
        String title,
        String description,
        Integer cookingTime,
        RecipeImportType importType,
        RecipeType recipeType,
        boolean completed,
        NutritionSummary nutrition,
        List<RecipeIngredientResponse> ingredients,
        List<RecipeStepResponse> steps
) {
}
