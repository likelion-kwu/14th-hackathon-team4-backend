package com.glucobite.auth.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenCodecTest {

    private final RefreshTokenCodec codec = new RefreshTokenCodec();

    @Test
    void issuesUrlSafeRandomTokenAndSha256Hash() {
        IssuedRefreshToken issuedToken = codec.issue();

        assertThat(issuedToken.rawToken()).hasSize(43);
        assertThat(issuedToken.rawToken()).matches("[A-Za-z0-9_-]+");
        assertThat(issuedToken.tokenHash()).hasSize(64);
        assertThat(issuedToken.tokenHash()).matches("[0-9a-f]+");
        assertThat(codec.hash(issuedToken.rawToken())).isEqualTo(issuedToken.tokenHash());
    }

    @Test
    void doesNotReuseIssuedRawTokens() {
        Set<String> issuedTokens = new HashSet<>();

        for (int index = 0; index < 100; index++) {
            issuedTokens.add(codec.issue().rawToken());
        }

        assertThat(issuedTokens).hasSize(100);
    }

    @Test
    void rejectsMissingRawToken() {
        assertThatThrownBy(() -> codec.hash(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.hash("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
