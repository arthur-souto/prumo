package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.BoardColumnEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardColumnJpaRepository extends JpaRepository<BoardColumnEntity, UUID> {

    List<BoardColumnEntity> findByWorkspaceIdOrderByPosition(UUID workspaceId);
}
