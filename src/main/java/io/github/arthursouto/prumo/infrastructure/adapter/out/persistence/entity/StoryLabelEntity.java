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
@Table(name = "story_labels")
@Getter
@Setter
@NoArgsConstructor
public class StoryLabelEntity {

    @Id
    private UUID id;

    @Column(name = "story_id", nullable = false)
    private UUID storyId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int position;
}
