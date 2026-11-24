package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.ConnectedRepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectedRepositoryJpaRepository extends JpaRepository<ConnectedRepositoryEntity, UUID> {

    List<ConnectedRepositoryEntity> findByWorkspaceIdOrderByCreatedAt(UUID workspaceId);

    Optional<ConnectedRepositoryEntity> findByWorkspaceIdAndOwnerIgnoreCaseAndNameIgnoreCase(
            UUID workspaceId,
            String owner,
            String name
    );
}
