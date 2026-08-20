package com.glucobite.recommendation.controller;

import com.glucobite.common.config.OpenApiConfig;
import com.glucobite.common.exception.ApiErrorResponse;
import com.glucobite.recommendation.dto.TrendRecommendationResponse;
import com.glucobite.recommendation.service.TrendRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Trend Recommendation", description = "기록 추세 기반 권고 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class TrendRecommendationController {

    private final TrendRecommendationService trendRecommendationService;

    public TrendRecommendationController(TrendRecommendationService trendRecommendationService) {
        this.trendRecommendationService = trendRecommendationService;
    }

    @GetMapping("/trends")
    @Operation(summary = "추세 기반 추천", description = "최근 7~30일 기록과 HealthProfile 목표를 설명 가능한 규칙으로 분석합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = TrendRecommendationResponse.class))),
            @ApiResponse(responseCode = "400", description = "분석 기간 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "HealthProfile 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public TrendRecommendationResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "7") @Min(7) @Max(30) int days
    ) {
        return trendRecommendationService.get(Long.valueOf(jwt.getSubject()), days);
    }
}
