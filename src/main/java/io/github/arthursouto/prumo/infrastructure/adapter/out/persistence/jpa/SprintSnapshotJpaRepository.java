package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.SprintSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SprintSnapshotJpaRepository extends JpaRepository<SprintSnapshotEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO sprint_snapshots
                (id, sprint_id, snapshot_date, total_points, remaining_points, total_stories, remaining_stories, recorded_at)
            VALUES (:id, :sprintId, :date, :totalPoints, :remainingPoints, :totalStories, :remainingStories, :recordedAt)
            ON CONFLICT (sprint_id, snapshot_date) DO UPDATE SET
                total_points = EXCLUDED.total_points,
                remaining_points = EXCLUDED.remaining_points,
                total_stories = EXCLUDED.total_stories,
                remaining_stories = EXCLUDED.remaining_stories,
                recorded_at = EXCLUDED.recorded_at
            """, nativeQuery = true)
    void saveForDay(@Param("id") UUID id,
                    @Param("sprintId") UUID sprintId,
                    @Param("date") LocalDate date,
                    @Param("totalPoints") int totalPoints,
                    @Param("remainingPoints") int remainingPoints,
                    @Param("totalStories") int totalStories,
                    @Param("remainingStories") int remainingStories,
                    @Param("recordedAt") Instant recordedAt);

    List<SprintSnapshotEntity> findBySprintIdOrderBySnapshotDateAsc(UUID sprintId);
}
