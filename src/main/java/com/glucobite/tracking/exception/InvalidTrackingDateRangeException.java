package com.glucobite.tracking.exception;

public class InvalidTrackingDateRangeException extends RuntimeException {

    public InvalidTrackingDateRangeException() {
        super("조회 날짜 범위를 확인해 주세요.");
    }
}
