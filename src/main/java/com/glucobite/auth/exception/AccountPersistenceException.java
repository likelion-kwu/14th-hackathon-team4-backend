package com.glucobite.auth.exception;

public class AccountPersistenceException extends RuntimeException {

    public AccountPersistenceException(Throwable cause) {
        super("계정 정보를 저장하지 못했습니다.", cause);
    }
}
