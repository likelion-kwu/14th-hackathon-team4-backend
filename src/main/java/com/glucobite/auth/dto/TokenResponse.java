package com.glucobite.auth.dto;

import com.glucobite.common.security.IssuedToken;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    private static final String BEARER_TYPE = "Bearer";

    public static TokenResponse from(IssuedToken token) {
        return new TokenResponse(token.accessToken(), BEARER_TYPE, token.expiresIn());
    }
}
