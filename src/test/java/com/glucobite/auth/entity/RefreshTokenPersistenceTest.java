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
    void persistsHashWithoutRawTokenColumn() {
        User user = persistUser("refresh-token-user");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        RefreshToken token = entityManager.persistFlushFind(new RefreshToken(
                user,
                "a".repeat(64),
                "72a95192-7ec7-4493-866a-c0e58735d1e0",
                expiresAt
        ));

        assertThat(token.getTokenHash()).isEqualTo("a".repeat(64));
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getRevokedAt()).isNull();
        assertThat(token.getReplacedByToken()).isNull();
    }

    @Test
    void linksReplacementWhenRotated() {
        User user = persistUser("rotated-refresh-token-user");
        LocalDateTime now = LocalDateTime.now();
        String familyId = "2b2aa96e-83d3-43b6-924f-dce002322e37";
        RefreshToken original = entityManager.persistAndFlush(new RefreshToken(
                user, "b".repeat(64), familyId, now.plusDays(30)
        ));
        RefreshToken replacement = entityManager.persistAndFlush(new RefreshToken(
                user, "c".repeat(64), familyId, now.plusDays(30)
        ));

        original.revoke(now, replacement);
        entityManager.flush();
        entityManager.clear();

        RefreshToken saved = entityManager.find(RefreshToken.class, original.getId());
        assertThat(saved.isRevoked()).isTrue();
        assertThat(saved.getReplacedByToken().getId()).isEqualTo(replacement.getId());
    }

    @Test
    void treatsBoundaryExpirationAsExpired() {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = new RefreshToken(
                new User("expired-user", "hash", "만료 사용자"),
                "d".repeat(64),
                "0bf60426-9194-42ef-9537-f16a38629687",
                now
        );

        assertThat(token.isExpired(now)).isTrue();
    }

    private User persistUser(String loginId) {
        return entityManager.persistAndFlush(new User(loginId, "hash", "인증 사용자"));
    }
}
