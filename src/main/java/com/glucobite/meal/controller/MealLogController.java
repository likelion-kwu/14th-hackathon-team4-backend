package com.glucobite.meal.controller;

import com.glucobite.common.config.OpenApiConfig;
import com.glucobite.common.exception.ApiErrorResponse;
import com.glucobite.meal.dto.CreateMealLogRequest;
import com.glucobite.meal.dto.MealLogListResponse;
import com.glucobite.meal.dto.MealLogResponse;
import com.glucobite.meal.service.MealLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/meal-logs")
@Tag(name = "Meal Log", description = "식사 기록 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class MealLogController {

    private final MealLogService mealLogService;

    public MealLogController(MealLogService mealLogService) {
        this.mealLogService = mealLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "식사 기록", description = "내 Recipe 또는 직접 입력 영양값을 시점 snapshot으로 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "기록 성공",
                    content = @Content(schema = @Schema(implementation = MealLogResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 validation 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "소유한 Recipe 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public MealLogResponse create(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMealLogRequest request
    ) {
        return mealLogService.create(Long.valueOf(jwt.getSubject()), request);
    }

    @GetMapping
    @Operation(summary = "식사 기록 조회", description = "KST 날짜 기준 최대 31일의 내 식사 기록을 최신순으로 조회합니다.")
    public MealLogListResponse get(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return mealLogService.get(Long.valueOf(jwt.getSubject()), from, to);
    }
}
