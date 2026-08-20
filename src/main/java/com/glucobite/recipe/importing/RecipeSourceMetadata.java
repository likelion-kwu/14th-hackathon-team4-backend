package com.glucobite.recipe.importing;

public record RecipeSourceMetadata(
        String sourceUrl,
        String externalId,
        String imageUrl
) {

    public static RecipeSourceMetadata none() {
        return new RecipeSourceMetadata(null, null, null);
    }
}
