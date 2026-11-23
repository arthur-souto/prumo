package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepositoryPort {

    Workspace save(Workspace workspace);

    Optional<Workspace> findById(UUID id);

    List<Workspace> findAllByMemberUserId(UUID userId);

    boolean existsBySlug(String slug);

    void deleteById(UUID id);
}
