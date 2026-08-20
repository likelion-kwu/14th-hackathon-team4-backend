package com.glucobite.recipe.exception;

public class InvalidSubstituteIngredientException extends RuntimeException {

    public InvalidSubstituteIngredientException() {
        super("해당 재료로 대체할 수 없습니다.");
    }
}
