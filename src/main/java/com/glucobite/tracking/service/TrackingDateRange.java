package com.glucobite.tracking.service;

import com.glucobite.tracking.exception.InvalidTrackingDateRangeException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public record TrackingDateRange(
        LocalDate from,
        LocalDate to,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive
) {
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_DAYS = 31;

    public static TrackingDateRange resolve(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            LocalDate today = LocalDate.now(KST);
            return of(today, today);
        }
        if (from == null || to == null) {
            throw new InvalidTrackingDateRangeException();
        }
        return of(from, to);
    }

    public static TrackingDateRange of(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days < 1 || days > MAX_DAYS) {
            throw new InvalidTrackingDateRangeException();
        }
        return new TrackingDateRange(
                from,
                to,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        );
    }
}
