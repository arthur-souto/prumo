package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationCodeRepositoryPort {

    AuthorizationCode save(AuthorizationCode authorizationCode);

    Optional<AuthorizationCode> findByCodeHash(String codeHash);

    boolean markUsed(UUID id, Instant usedAt);
}
