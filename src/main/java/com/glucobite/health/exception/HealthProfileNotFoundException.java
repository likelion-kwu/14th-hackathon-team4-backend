package com.glucobite.health.exception;

public class HealthProfileNotFoundException extends RuntimeException {

    public HealthProfileNotFoundException() {
        super("건강 프로필 정보가 존재하지 않습니다.");
    }
}
