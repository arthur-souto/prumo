package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.WorkspaceMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WorkspaceMemberJpaRepository extends JpaRepository<WorkspaceMemberEntity, UUID> {

    List<WorkspaceMemberEntity> findByUserId(UUID userId);

    List<WorkspaceMemberEntity> findByWorkspaceId(UUID workspaceId);

    List<WorkspaceMemberEntity> findByWorkspaceIdIn(Collection<UUID> workspaceIds);
}
