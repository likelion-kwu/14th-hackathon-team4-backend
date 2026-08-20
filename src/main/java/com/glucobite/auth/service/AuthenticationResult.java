package com.glucobite.auth.service;

import com.glucobite.auth.dto.TokenResponse;

public record AuthenticationResult(
        TokenResponse accessToken,
        String refreshToken
) {
}
