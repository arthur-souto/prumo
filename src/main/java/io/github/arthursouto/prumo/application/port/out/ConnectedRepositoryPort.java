package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectedRepositoryPort {

    ConnectedRepository save(ConnectedRepository repository);

    Optional<ConnectedRepository> findById(UUID id);

    List<ConnectedRepository> findAllByWorkspaceId(UUID workspaceId);

    List<ConnectedRepository> findAll();

    Optional<ConnectedRepository> findByWorkspaceIdAndOwnerAndName(UUID workspaceId, String owner, String name);

    void deleteById(UUID id);
}
