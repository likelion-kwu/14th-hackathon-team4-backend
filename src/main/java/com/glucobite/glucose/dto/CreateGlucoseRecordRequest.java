package com.glucobite.glucose.dto;

import com.glucobite.glucose.entity.GlucoseMeasurementContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "혈당 기록 생성 요청")
public record CreateGlucoseRecordRequest(
        Long mealLogId,
        @NotNull @DecimalMin("20.00") @DecimalMax("600.00")
        @Digits(integer = 3, fraction = 2) BigDecimal value,
        @NotNull GlucoseMeasurementContext context,
        @NotNull @PastOrPresent LocalDateTime measuredAt
) {
}
