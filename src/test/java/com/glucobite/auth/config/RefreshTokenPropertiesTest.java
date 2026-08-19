package com.glucobite.auth.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenPropertiesTest {

    @Test
    void rejectsNonPositiveExpiration() {
        assertThatThrownBy(() -> properties(Duration.ZERO, true, "Lax"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties(Duration.ofDays(-1), true, "Lax"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedSameSitePolicy() {
        assertThatThrownBy(() -> properties(Duration.ofDays(30), true, "Invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInsecureSameSiteNoneCookie() {
        assertThatThrownBy(() -> properties(Duration.ofDays(30), false, "None"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RefreshTokenProperties properties(
            Duration expiration,
            boolean secure,
            String sameSite
    ) {
        return new RefreshTokenProperties(
                expiration,
                "refresh_token",
                secure,
                sameSite,
                null
        );
    }
}
