package com.glucobite.recipe.exception;

public class RecipeIngredientNotFoundException extends RuntimeException {

    public RecipeIngredientNotFoundException() {
        super("레시피에 해당 재료가 존재하지 않습니다.");
    }
}
