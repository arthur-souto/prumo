package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import java.util.Optional;
import java.util.UUID;

public interface GithubConnectionRepositoryPort {

    GithubConnection save(GithubConnection connection);

    Optional<GithubConnection> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
