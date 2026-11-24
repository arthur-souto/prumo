package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.StoryChecklistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StoryChecklistItemJpaRepository extends JpaRepository<StoryChecklistItemEntity, UUID> {

    List<StoryChecklistItemEntity> findByStoryIdIn(Collection<UUID> storyIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StoryChecklistItemEntity item where item.storyId = :storyId")
    void deleteByStoryId(@Param("storyId") UUID storyId);
}
