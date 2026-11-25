package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintSnapshot;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.SprintPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.SprintSnapshotPersistenceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SprintPersistenceAdapter.class, SprintSnapshotPersistenceAdapter.class})
class SprintSnapshotPersistenceIT {

    private static final LocalDate DAY = LocalDate.of(2026, 12, 1);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private SprintPersistenceAdapter sprints;

    @Autowired
    private SprintSnapshotPersistenceAdapter snapshots;

    @Autowired
    private JdbcTemplate jdbc;

    private Sprint sprint;

    private UUID workspace() {
        UUID owner = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'U', ?, 'USER', true, now(), now())",
                owner, owner + "@test.dev");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO workspaces (id, name, slug, owner_id, created_at, updated_at) VALUES (?, 'W', ?, ?, now(), now())",
                id, "w-" + id, owner);
        return id;
    }

    @BeforeEach
    void setUp() {
        sprint = sprints.save(Sprint.create(workspace(), "Sprint", null, DAY, DAY.plusDays(13)));
    }

    private void record(LocalDate date, int total, int remaining) {
        snapshots.saveForDay(SprintSnapshot.of(sprint.getId(), date, total, remaining, 4, 2, Instant.now()));
    }

    @Test
    void keepsOneSnapshotPerDayWithTheLatestValues() {
        record(DAY, 20, 20);
        record(DAY, 21, 18);

        assertThat(snapshots.findBySprintId(sprint.getId()))
                .extracting(SprintSnapshot::date, SprintSnapshot::totalPoints, SprintSnapshot::remainingPoints)
                .containsExactly(tuple(DAY, 21, 18));
    }

    @Test
    void returnsTheSnapshotsInDateOrder() {
        record(DAY.plusDays(2), 20, 8);
        record(DAY, 20, 20);
        record(DAY.plusDays(1), 20, 13);

        assertThat(snapshots.findBySprintId(sprint.getId()))
                .extracting(SprintSnapshot::date, SprintSnapshot::remainingPoints)
                .containsExactly(tuple(DAY, 20), tuple(DAY.plusDays(1), 13), tuple(DAY.plusDays(2), 8));
    }

    @Test
    void deletingTheSprintDeletesItsSnapshots() {
        record(DAY, 20, 20);

        sprints.delete(sprint);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM sprint_snapshots WHERE sprint_id = ?", Integer.class, sprint.getId())).isZero();
    }

    @Test
    void findsTheActiveSprintsOfEveryWorkspace() {
        sprint.start(0);
        sprints.save(sprint);
        Sprint other = Sprint.create(workspace(), "Other", null, DAY, DAY);
        other.start(0);
        sprints.save(other);
        sprints.save(Sprint.create(sprint.getWorkspaceId(), "Planned", null, DAY, DAY));

        assertThat(sprints.findAllActive()).extracting(Sprint::getId).contains(sprint.getId(), other.getId())
                .allSatisfy(id -> assertThat(sprints.findById(id).orElseThrow().isActive()).isTrue());
    }
}
