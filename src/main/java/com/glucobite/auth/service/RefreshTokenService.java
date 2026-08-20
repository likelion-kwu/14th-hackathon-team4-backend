package com.glucobite.auth.service;

import com.glucobite.auth.config.RefreshTokenProperties;
import com.glucobite.auth.entity.RefreshToken;
import com.glucobite.auth.exception.InvalidRefreshTokenException;
import com.glucobite.auth.repository.RefreshTokenRepository;
import com.glucobite.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCodec tokenCodec;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    @Autowired
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenCodec tokenCodec,
            RefreshTokenProperties properties
    ) {
        this(refreshTokenRepository, tokenCodec, properties, Clock.systemUTC());
    }

    RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenCodec tokenCodec,
            RefreshTokenProperties properties,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenCodec = tokenCodec;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public String issue(User user) {
        return issue(user, UUID.randomUUID().toString()).rawToken();
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshToken current = findForUpdate(rawToken);
        LocalDateTime now = now();

        if (current.isRevoked()) {
            revokeFamily(current.getTokenFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now)) {
            current.revoke(now, null);
            throw new InvalidRefreshTokenException();
        }

        IssuedTokenEntity replacement = issue(current.getUser(), current.getTokenFamilyId());
        current.revoke(now, replacement.entity());
        return new RotatedRefreshToken(current.getUser(), replacement.rawToken());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(tokenCodec.hash(rawToken))
                .ifPresent(token -> token.revoke(now(), null));
    }

    private RefreshToken findForUpdate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return refreshTokenRepository.findByTokenHashForUpdate(tokenCodec.hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
    }

    private IssuedTokenEntity issue(User user, String familyId) {
        IssuedRefreshToken issued = tokenCodec.issue();
        RefreshToken entity = refreshTokenRepository.save(new RefreshToken(
                user,
                issued.tokenHash(),
                familyId,
                now().plus(properties.expiration())
        ));
        return new IssuedTokenEntity(issued.rawToken(), entity);
    }

    private void revokeFamily(String familyId, LocalDateTime now) {
        refreshTokenRepository.findAllByTokenFamilyIdAndRevokedAtIsNull(familyId)
                .forEach(token -> token.revoke(now, null));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private record IssuedTokenEntity(String rawToken, RefreshToken entity) {
    }
}
