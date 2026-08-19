package com.glucobite.auth.dto;

import com.glucobite.common.security.IssuedToken;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 액세스 토큰 응답")
public record TokenResponse(
        @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature")
        String accessToken,

        @Schema(description = "Authorization 헤더 인증 유형", example = "Bearer")
        String tokenType,

        @Schema(description = "토큰 만료까지 남은 시간(초)", example = "3600")
        long expiresIn
) {

    private static final String BEARER_TYPE = "Bearer";

    public static TokenResponse from(IssuedToken token) {
        return new TokenResponse(token.accessToken(), BEARER_TYPE, token.expiresIn());
    }
}
