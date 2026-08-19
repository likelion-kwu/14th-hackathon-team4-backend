package com.glucobite.health.exception;

public class HealthProfileNotFoundException extends RuntimeException {

    public HealthProfileNotFoundException() {
        super("건강 프로필을 찾을 수 없습니다.");
    }
}
