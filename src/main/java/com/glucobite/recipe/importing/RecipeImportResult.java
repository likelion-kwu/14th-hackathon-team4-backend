package com.glucobite.recipe.importing;

import com.glucobite.recipe.dto.ImportedRecipeResponse;

public record RecipeImportResult(
        ImportedRecipeResponse response,
        boolean created
) {
}
