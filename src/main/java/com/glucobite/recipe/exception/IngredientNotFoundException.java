package com.glucobite.recipe.exception;

public class IngredientNotFoundException extends RuntimeException {

    public IngredientNotFoundException() {
        super("해당 재료를 찾을 수 없습니다.");
    }
}
