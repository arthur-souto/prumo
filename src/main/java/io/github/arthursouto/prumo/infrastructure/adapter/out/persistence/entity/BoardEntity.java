package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
public class BoardEntity {

    @Id
    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix;

    @Column(name = "next_story_number", nullable = false)
    private int nextStoryNumber;

    @Column(name = "automation_enabled", nullable = false)
    private boolean automationEnabled;

    @Column(name = "consolidate_stories", nullable = false)
    private boolean consolidateStories;
}
