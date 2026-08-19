package com.glucobite.auth.exception;

public class InvalidAllergenException extends RuntimeException {

    public InvalidAllergenException() {
        super("존재하지 않는 알레르기 항목이 포함되어 있습니다.");
    }
}
