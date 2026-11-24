package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.ActivityEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityJpaRepository extends JpaRepository<ActivityEntity, UUID> {

    List<ActivityEntity> findByStoryIdOrderByCreatedAtDesc(UUID storyId, Pageable pageable);

    List<ActivityEntity> findByWorkspaceIdAndSubtaskKeyOrderByCreatedAtDesc(UUID workspaceId, String subtaskKey, Pageable pageable);
}
