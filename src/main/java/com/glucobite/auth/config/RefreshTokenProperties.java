package com.glucobite.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "app.auth.refresh-token")
public record RefreshTokenProperties(
        @NotNull Duration expiration,
        @NotBlank String cookieName,
        boolean cookieSecure,
        @NotBlank String cookieSameSite,
        String cookieDomain
) {

    private static final Set<String> ALLOWED_SAME_SITE = Set.of("strict", "lax", "none");

    public RefreshTokenProperties {
        if (expiration != null && (expiration.isZero() || expiration.isNegative())) {
            throw new IllegalArgumentException("Refresh token expiration must be positive");
        }
        if (cookieSameSite != null
                && !ALLOWED_SAME_SITE.contains(cookieSameSite.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Refresh token SameSite must be Strict, Lax, or None");
        }
        if ("none".equalsIgnoreCase(cookieSameSite) && !cookieSecure) {
            throw new IllegalArgumentException("SameSite=None refresh token cookies must be secure");
        }
        if (cookieDomain != null && cookieDomain.isBlank()) {
            cookieDomain = null;
        }
    }
}
