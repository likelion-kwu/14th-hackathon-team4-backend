package com.glucobite.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("인증을 갱신할 수 없습니다. 다시 로그인해 주세요.");
    }
}
