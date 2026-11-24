package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.PullRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PullRequestJpaRepository extends JpaRepository<PullRequestEntity, UUID> {

    List<PullRequestEntity> findByStoryIdIn(Collection<UUID> storyIds);

    Optional<PullRequestEntity> findByStoryIdAndRepositoryIdAndNumber(UUID storyId, UUID repositoryId, int number);
}
