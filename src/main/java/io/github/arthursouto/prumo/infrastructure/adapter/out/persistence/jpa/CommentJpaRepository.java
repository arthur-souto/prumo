package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentJpaRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByStoryIdAndSubtaskKeyIsNullOrderByCreatedAtAsc(UUID storyId);

    List<CommentEntity> findByWorkspaceIdAndSubtaskKeyOrderByCreatedAtAsc(UUID workspaceId, String subtaskKey);
}
