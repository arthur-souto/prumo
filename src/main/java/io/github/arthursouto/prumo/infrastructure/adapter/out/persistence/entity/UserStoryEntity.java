package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity;

import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_stories")
@Getter
@Setter
@NoArgsConstructor
public class UserStoryEntity {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "story_number", nullable = false)
    private int number;

    @Column(name = "story_key", nullable = false)
    private String storyKey;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoryPriority priority;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "column_id", nullable = false)
    private UUID columnId;

    @Column(name = "sort_rank", nullable = false)
    private String sortRank;

    @Column(name = "repository_id")
    private UUID repositoryId;

    @Column(name = "assignee_id")
    private UUID assigneeId;


    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }
}
