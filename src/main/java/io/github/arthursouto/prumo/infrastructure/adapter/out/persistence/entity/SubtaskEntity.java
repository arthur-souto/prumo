package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity;

import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
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
@Table(name = "story_subtasks")
@Getter
@Setter
@NoArgsConstructor
public class SubtaskEntity {

    @Id
    private UUID id;

    @Column(name = "story_id", nullable = false)
    private UUID storyId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "subtask_number", nullable = false)
    private int number;

    @Column(name = "subtask_key", nullable = false)
    private String subtaskKey;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubtaskStatus status;

    @Column(nullable = false)
    private int position;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
