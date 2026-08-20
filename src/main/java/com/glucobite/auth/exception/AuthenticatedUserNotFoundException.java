package com.glucobite.auth.exception;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException() {
        super("인증된 사용자를 찾을 수 없습니다.");
    }
}
