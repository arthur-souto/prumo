package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.StoryLabelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StoryLabelJpaRepository extends JpaRepository<StoryLabelEntity, UUID> {

    List<StoryLabelEntity> findByStoryIdIn(Collection<UUID> storyIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StoryLabelEntity label where label.storyId = :storyId")
    void deleteByStoryId(@Param("storyId") UUID storyId);
}
