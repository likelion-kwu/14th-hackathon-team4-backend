package com.glucobite.health.controller;

import com.glucobite.common.config.OpenApiConfig;
import com.glucobite.common.exception.ApiErrorResponse;
import com.glucobite.health.dto.HealthProfileResponse;
import com.glucobite.health.dto.HealthProfileUpdateRequest;
import com.glucobite.health.service.HealthProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/profile")
@Tag(name = "Health Profile", description = "내 건강 프로필 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class HealthProfileController {

    private final HealthProfileService healthProfileService;

    public HealthProfileController(HealthProfileService healthProfileService) {
        this.healthProfileService = healthProfileService;
    }

    @GetMapping
    @Operation(
            summary = "내 건강 프로필 조회",
            description = "로그인 사용자가 회원가입에서 저장한 건강 설정과 알레르기를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = HealthProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰 누락 또는 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "건강 프로필 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public HealthProfileResponse getProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return healthProfileService.getProfile(Long.valueOf(jwt.getSubject()));
    }

    @PutMapping
    @Operation(
            summary = "내 건강 프로필 수정",
            description = "현재 건강 설정 전체를 제출해 기존 프로필을 교체합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = HealthProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 validation 실패 또는 존재하지 않는 알레르기",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰 누락 또는 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "건강 프로필 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public HealthProfileResponse updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody HealthProfileUpdateRequest request
    ) {
        return healthProfileService.updateProfile(Long.valueOf(jwt.getSubject()), request);
    }
}
