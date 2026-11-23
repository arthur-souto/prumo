package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.auth.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void rotate(RefreshToken current, RefreshToken next);

    void revokeFamily(UUID familyId, Instant revokedAt);
}
