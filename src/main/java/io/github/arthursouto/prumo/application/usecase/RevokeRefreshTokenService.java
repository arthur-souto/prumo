package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.RevokeRefreshTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.command.RevokeRefreshTokenCommand;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenCodecPort;
import io.github.arthursouto.prumo.application.port.out.RefreshTokenRepositoryPort;

import java.time.Clock;

public class RevokeRefreshTokenService implements RevokeRefreshTokenUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final RefreshTokenCodecPort refreshTokenCodec;
    private final Clock clock;

    public RevokeRefreshTokenService(RefreshTokenRepositoryPort refreshTokenRepository,
                                     RefreshTokenCodecPort refreshTokenCodec,
                                     Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenCodec = refreshTokenCodec;
        this.clock = clock;
    }

    @Override
    public void execute(RevokeRefreshTokenCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            return;
        }

        refreshTokenRepository
                .findByTokenHash(refreshTokenCodec.hash(command.refreshToken()))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant()));
    }
}
