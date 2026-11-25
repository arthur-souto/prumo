package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sprint_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class SprintSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Column(name = "remaining_points", nullable = false)
    private int remainingPoints;

    @Column(name = "total_stories", nullable = false)
    private int totalStories;

    @Column(name = "remaining_stories", nullable = false)
    private int remainingStories;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
