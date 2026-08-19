package com.glucobite.common.security;

public record IssuedToken(
        String accessToken,
        long expiresIn
) {
}
