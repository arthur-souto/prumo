package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.application.port.out.AccessTokenIssuerPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenCodecPort;
import io.github.arthursouto.prumo.domain.model.auth.AccessToken;
import io.github.arthursouto.prumo.domain.model.auth.RefreshToken;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

final class TokenIssuer {

    private final AccessTokenIssuerPort accessTokenIssuer;
    private final RefreshTokenCodecPort refreshTokenCodec;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    TokenIssuer(AccessTokenIssuerPort accessTokenIssuer,
                RefreshTokenCodecPort refreshTokenCodec,
                Clock clock,
                Duration refreshTokenTtl) {
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenCodec = refreshTokenCodec;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    IssuedPair issue(User user, UUID familyId) {
        Instant now = clock.instant();
        String rawRefreshToken = refreshTokenCodec.generate();

        RefreshToken refreshToken = RefreshToken.issue(
                user.getId(),
                familyId,
                refreshTokenCodec.hash(rawRefreshToken),
                now,
                now.plus(refreshTokenTtl)
        );
        AccessToken accessToken = accessTokenIssuer.issue(user);

        return new IssuedPair(
                refreshToken,
                new AuthTokens(user, accessToken, rawRefreshToken, refreshToken.getExpiresAt())
        );
    }

    record IssuedPair(RefreshToken refreshToken, AuthTokens tokens) {
    }
}
