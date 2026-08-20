package com.glucobite.recipe.exception;

public class RecipePersonalizationGenerationException extends RuntimeException {

    public RecipePersonalizationGenerationException(String message) {
        super(message);
    }

    public RecipePersonalizationGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
