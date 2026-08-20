package com.glucobite.glucose.dto;

import java.time.LocalDate;
import java.util.List;

public record GlucoseRecordListResponse(
        LocalDate from,
        LocalDate to,
        List<GlucoseRecordResponse> records
) {
}
