package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.InvalidRefreshTokenException;
import io.github.arthursouto.prumo.application.port.in.RefreshTokensUseCase;
import io.github.arthursouto.prumo.application.port.in.command.RefreshTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;
import io.github.arthursouto.prumo.application.port.out.AccessTokenIssuerPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenCodecPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.auth.RefreshToken;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class RefreshTokensService implements RefreshTokensUseCase {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final RefreshTokenCodecPort refreshTokenCodec;
    private final Clock clock;
    private final TokenIssuer tokenIssuer;

    public RefreshTokensService(UserRepositoryPort userRepository,
                                RefreshTokenRepositoryPort refreshTokenRepository,
                                AccessTokenIssuerPort accessTokenIssuer,
                                RefreshTokenCodecPort refreshTokenCodec,
                                Clock clock,
                                Duration refreshTokenTtl) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenCodec = refreshTokenCodec;
        this.clock = clock;
        this.tokenIssuer = new TokenIssuer(accessTokenIssuer, refreshTokenCodec, clock, refreshTokenTtl);
    }

    @Override
    public AuthTokens execute(RefreshTokensCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken current = refreshTokenRepository
                .findByTokenHash(refreshTokenCodec.hash(command.refreshToken()))
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = clock.instant();

        if (current.isRevoked()) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(current.getUserId()).orElseThrow(InvalidRefreshTokenException::new);

        if (!user.isEnabled()) {
            throw new InvalidRefreshTokenException();
        }

        TokenIssuer.IssuedPair issued = tokenIssuer.issue(user, current.getFamilyId());
        current.revoke(now);
        refreshTokenRepository.rotate(current, issued.refreshToken());

        return issued.tokens();
    }
}
