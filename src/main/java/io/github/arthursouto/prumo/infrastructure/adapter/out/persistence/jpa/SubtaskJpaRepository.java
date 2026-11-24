package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.SubtaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubtaskJpaRepository extends JpaRepository<SubtaskEntity, UUID> {

    Optional<SubtaskEntity> findByWorkspaceIdAndSubtaskKey(UUID workspaceId, String subtaskKey);

    List<SubtaskEntity> findByStoryIdOrderByPositionAsc(UUID storyId);

    List<SubtaskEntity> findByStoryIdIn(Collection<UUID> storyIds);

    List<SubtaskEntity> findByWorkspaceIdAndSubtaskKeyIn(UUID workspaceId, Collection<String> keys);
}
