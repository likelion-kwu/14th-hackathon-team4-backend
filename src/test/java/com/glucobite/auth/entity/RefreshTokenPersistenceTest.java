package com.glucobite.auth.entity;

import com.glucobite.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsOnlyHashAndTokenFamilyMetadata() {
        User user = persistUser("refresh-token-user");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        RefreshToken token = entityManager.persistAndFlush(new RefreshToken(
                user,
                "a".repeat(64),
                "72a95192-7ec7-4493-866a-c0e58735d1e0",
                expiresAt
        ));
        entityManager.clear();

        RefreshToken savedToken = entityManager.find(RefreshToken.class, token.getId());

        assertThat(savedToken.getTokenHash()).isEqualTo("a".repeat(64));
        assertThat(savedToken.getTokenFamilyId())
                .isEqualTo("72a95192-7ec7-4493-866a-c0e58735d1e0");
        assertThat(savedToken.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(savedToken.getRevokedAt()).isNull();
        assertThat(savedToken.getReplacedByToken()).isNull();
    }

    @Test
    void linksRotatedTokenAndMarksOriginalInactive() {
        User user = persistUser("rotated-refresh-token-user");
        LocalDateTime now = LocalDateTime.now();
        String familyId = "2b2aa96e-83d3-43b6-924f-dce002322e37";
        RefreshToken original = entityManager.persistAndFlush(new RefreshToken(
                user,
                "b".repeat(64),
                familyId,
                now.plusDays(30)
        ));
        RefreshToken replacement = entityManager.persistAndFlush(new RefreshToken(
                user,
                "c".repeat(64),
                familyId,
                now.plusDays(30)
        ));

        original.revoke(now, replacement);
        entityManager.flush();
        entityManager.clear();

        RefreshToken savedOriginal = entityManager.find(RefreshToken.class, original.getId());
        assertThat(savedOriginal.isActive(now.plusSeconds(1))).isFalse();
        assertThat(savedOriginal.getRevokedAt()).isEqualTo(now);
        assertThat(savedOriginal.getReplacedByToken().getId()).isEqualTo(replacement.getId());
    }

    @Test
    void considersExpiredTokenInactive() {
        User user = new User("expired-user", "encoded-password", "만료 사용자");
        LocalDateTime now = LocalDateTime.now();
        RefreshToken expiredToken = new RefreshToken(
                user,
                "d".repeat(64),
                "0bf60426-9194-42ef-9537-f16a38629687",
                now
        );

        assertThat(expiredToken.isActive(now)).isFalse();
    }

    private User persistUser(String loginId) {
        return entityManager.persistAndFlush(
                new User(loginId, "encoded-password", "인증 사용자")
        );
    }
}
