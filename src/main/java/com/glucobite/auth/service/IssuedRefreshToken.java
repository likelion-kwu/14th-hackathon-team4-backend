package com.glucobite.auth.service;

public record IssuedRefreshToken(
        String rawToken,
        String tokenHash
) {
}
