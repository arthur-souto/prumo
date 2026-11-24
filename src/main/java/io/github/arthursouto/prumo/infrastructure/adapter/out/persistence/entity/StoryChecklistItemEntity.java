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
@Table(name = "story_checklist_items")
@Getter
@Setter
@NoArgsConstructor
public class StoryChecklistItemEntity {

    @Id
    private UUID id;

    @Column(name = "story_id", nullable = false)
    private UUID storyId;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false)
    private int position;
}
