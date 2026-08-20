package com.glucobite.meal.exception;

public class MealLogNotFoundException extends RuntimeException {

    public MealLogNotFoundException() {
        super("존재하지 않는 식사 기록입니다.");
    }
}
