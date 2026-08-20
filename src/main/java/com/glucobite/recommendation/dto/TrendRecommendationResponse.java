package com.glucobite.recommendation.dto;

import java.time.LocalDate;
import java.util.List;

public record TrendRecommendationResponse(
        LocalDate from,
        LocalDate to,
        int days,
        boolean dataSufficient,
        TrendMetricsResponse metrics,
        List<TrendRecommendationItemResponse> recommendations,
        String disclaimer
) {
}
