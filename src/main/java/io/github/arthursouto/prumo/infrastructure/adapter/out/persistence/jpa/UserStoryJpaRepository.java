package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.UserStoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStoryJpaRepository extends JpaRepository<UserStoryEntity, UUID> {

    Optional<UserStoryEntity> findByWorkspaceIdAndStoryKey(UUID workspaceId, String storyKey);

    List<UserStoryEntity> findByWorkspaceId(UUID workspaceId);

    List<UserStoryEntity> findByColumnId(UUID columnId);

    List<UserStoryEntity> findBySprintIdIn(Collection<UUID> sprintIds);

    List<UserStoryEntity> findByWorkspaceIdAndStoryKeyIn(UUID workspaceId, Collection<String> storyKeys);

    long countByWorkspaceId(UUID workspaceId);
}
