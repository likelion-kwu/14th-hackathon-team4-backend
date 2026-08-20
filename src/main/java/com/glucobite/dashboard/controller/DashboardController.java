package com.glucobite.dashboard.controller;

import com.glucobite.common.config.OpenApiConfig;
import com.glucobite.common.exception.ApiErrorResponse;
import com.glucobite.dashboard.dto.TodaySummaryResponse;
import com.glucobite.dashboard.service.TodaySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "일일 건강 요약 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class DashboardController {

    private final TodaySummaryService todaySummaryService;

    public DashboardController(TodaySummaryService todaySummaryService) {
        this.todaySummaryService = todaySummaryService;
    }

    @GetMapping("/today")
    @Operation(summary = "오늘 요약", description = "KST 오늘의 영양, 목표 진행률, 혈당, 식사 목록을 집계합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = TodaySummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "HealthProfile 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public TodaySummaryResponse today(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return todaySummaryService.get(Long.valueOf(jwt.getSubject()));
    }
}
