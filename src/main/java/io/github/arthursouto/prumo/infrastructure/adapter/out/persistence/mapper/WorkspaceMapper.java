package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper;

import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import io.github.arthursouto.prumo.domain.model.workspace.WorkspaceMember;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.WorkspaceEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.WorkspaceMemberEntity;

import java.util.List;

public final class WorkspaceMapper {

    private WorkspaceMapper() {
    }

    public static WorkspaceEntity toEntity(Workspace workspace) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setId(workspace.getId());
        entity.setName(workspace.getName());
        entity.setSlug(workspace.getSlug());
        entity.setDescription(workspace.getDescription());
        entity.setOwnerId(workspace.getOwnerId());
        entity.setCreatedAt(workspace.getCreatedAt());
        entity.setUpdatedAt(workspace.getUpdatedAt());
        return entity;
    }

    public static WorkspaceMemberEntity toEntity(WorkspaceMember member) {
        WorkspaceMemberEntity entity = new WorkspaceMemberEntity();
        entity.setId(member.getId());
        entity.setWorkspaceId(member.getWorkspaceId());
        entity.setUserId(member.getUserId());
        entity.setRole(member.getRole());
        entity.setCreatedAt(member.getCreatedAt());
        return entity;
    }

    public static WorkspaceMember toDomain(WorkspaceMemberEntity entity) {
        return WorkspaceMember.restore(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getUserId(),
                entity.getRole(),
                entity.getCreatedAt()
        );
    }

    public static Workspace toDomain(WorkspaceEntity entity, List<WorkspaceMemberEntity> members) {
        return Workspace.restore(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getOwnerId(),
                members.stream().map(WorkspaceMapper::toDomain).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
