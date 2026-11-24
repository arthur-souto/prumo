package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity;

import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
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
@Table(name = "story_pull_requests")
@Getter
@Setter
@NoArgsConstructor
public class PullRequestEntity {

    @Id
    private UUID id;

    @Column(name = "story_id", nullable = false)
    private UUID storyId;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PullRequestState state;

    @Column(nullable = false)
    private String url;

    private String branch;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
