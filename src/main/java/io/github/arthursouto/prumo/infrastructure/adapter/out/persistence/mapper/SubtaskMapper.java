package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper;

import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.SubtaskEntity;

public final class SubtaskMapper {

    private SubtaskMapper() {
    }

    public static SubtaskEntity toEntity(Subtask subtask) {
        SubtaskEntity entity = new SubtaskEntity();
        entity.setId(subtask.getId());
        entity.setStoryId(subtask.getStoryId());
        entity.setWorkspaceId(subtask.getWorkspaceId());
        entity.setNumber(subtask.getNumber());
        entity.setSubtaskKey(subtask.getKey());
        entity.setTitle(subtask.getTitle());
        entity.setDescription(subtask.getDescription());
        entity.setStatus(subtask.getStatus());
        entity.setPosition(subtask.getPosition());
        entity.setBranchName(subtask.getBranchName());
        entity.setCreatedAt(subtask.getCreatedAt());
        entity.setUpdatedAt(subtask.getUpdatedAt());
        return entity;
    }

    public static Subtask toDomain(SubtaskEntity entity) {
        return Subtask.restore(
                entity.getId(),
                entity.getStoryId(),
                entity.getWorkspaceId(),
                entity.getNumber(),
                entity.getSubtaskKey(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getPosition(),
                entity.getBranchName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
