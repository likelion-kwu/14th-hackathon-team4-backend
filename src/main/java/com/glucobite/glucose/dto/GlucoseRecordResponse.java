package com.glucobite.glucose.dto;

import com.glucobite.glucose.entity.GlucoseMeasurementContext;
import com.glucobite.glucose.entity.GlucoseRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GlucoseRecordResponse(
        Long glucoseRecordId,
        Long mealLogId,
        BigDecimal value,
        GlucoseMeasurementContext context,
        LocalDateTime measuredAt
) {
    public static GlucoseRecordResponse from(GlucoseRecord record) {
        return new GlucoseRecordResponse(
                record.getId(),
                record.getMealLog() == null ? null : record.getMealLog().getId(),
                record.getValue(),
                record.getContext(),
                record.getMeasuredAt()
        );
    }
}
