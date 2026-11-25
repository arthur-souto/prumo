package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.activity.Activity;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.story.ChangeSource;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.ActivityPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.BoardPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.StoryPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.UserStorySearchRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.SubtaskPersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BoardPersistenceAdapter.class, StoryPersistenceAdapter.class, UserStorySearchRepository.class, SubtaskPersistenceAdapter.class, ActivityPersistenceAdapter.class})
class ActivityPersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private BoardPersistenceAdapter boards;

    @Autowired
    private StoryPersistenceAdapter stories;

    @Autowired
    private ActivityPersistenceAdapter activity;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private UUID userId;
    private UUID workspaceId;
    private UserStory story;

    private void givenAStory(String prefix) {
        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'U', ?, 'USER', true, now(), now())",
                userId, userId + "@test.dev");
        jdbc.update("INSERT INTO workspaces (id, name, slug, owner_id, created_at, updated_at) VALUES (?, 'W', ?, ?, now(), now())",
                workspaceId, "w-" + workspaceId, userId);
        Board board = boards.save(Board.withDefaults(workspaceId, prefix));
        int number = boards.allocateStoryNumber(workspaceId);
        story = stories.save(UserStory.create(workspaceId, number, prefix + "-" + number, "Story", null,
                StoryPriority.MEDIUM, null, board.ofKind(ColumnKind.TODO).orElseThrow().getId(), "V", List.of(), List.of()));
    }

    private Activity at(Instant when, String subtaskKey, UUID actor, ChangeSource source, ActivityType type, String from, String to) {
        return new Activity(UUID.randomUUID(), workspaceId, story.getId(), subtaskKey, actor, source, type, from, to, when);
    }

    @Test
    void theStoryHistoryComesBackNewestFirstWithItsSubtasksAndRespectsTheLimit() {
        givenAStory("ACT");
        Instant base = Instant.parse("2026-01-01T10:00:00Z");
        activity.save(at(base, null, userId, ChangeSource.USER, ActivityType.STORY_CREATED, null, "Story"));
        activity.save(at(base.plusSeconds(60), "ACT-2", userId, ChangeSource.USER, ActivityType.SUBTASK_ADDED, null, "Task"));
        activity.save(at(base.plusSeconds(120), "ACT-2", null, ChangeSource.GITHUB, ActivityType.SUBTASK_STATUS_CHANGED, "TO_DO", "IN_REVIEW"));
        entityManager.flush();
        entityManager.clear();

        assertThat(activity.findByStoryId(story.getId(), 10)).extracting(Activity::type)
                .containsExactly(ActivityType.SUBTASK_STATUS_CHANGED, ActivityType.SUBTASK_ADDED, ActivityType.STORY_CREATED);
        assertThat(activity.findByStoryId(story.getId(), 2)).hasSize(2);

        Activity automated = activity.findByStoryId(story.getId(), 1).get(0);
        assertThat(automated.actorId()).isNull();
        assertThat(automated.source()).isEqualTo(ChangeSource.GITHUB);
        assertThat(automated.fromValue()).isEqualTo("TO_DO");
        assertThat(automated.toValue()).isEqualTo("IN_REVIEW");
    }

    @Test
    void aSubtaskHistoryOnlyHoldsTheEntriesOfThatSubtaskInsideItsWorkspace() {
        givenAStory("SUB");
        Instant base = Instant.parse("2026-01-01T10:00:00Z");
        activity.save(at(base, "SUB-2", userId, ChangeSource.USER, ActivityType.SUBTASK_ADDED, null, "One"));
        activity.save(at(base.plusSeconds(5), "SUB-3", userId, ChangeSource.USER, ActivityType.SUBTASK_ADDED, null, "Two"));
        activity.save(at(base.plusSeconds(10), "SUB-2", userId, ChangeSource.USER, ActivityType.SUBTASK_STATUS_CHANGED, "TO_DO", "DONE"));
        entityManager.flush();
        entityManager.clear();

        assertThat(activity.findBySubtaskKey(workspaceId, "SUB-2", 10)).extracting(Activity::type)
                .containsExactly(ActivityType.SUBTASK_STATUS_CHANGED, ActivityType.SUBTASK_ADDED);
        assertThat(activity.findBySubtaskKey(UUID.randomUUID(), "SUB-2", 10)).isEmpty();
    }

    @Test
    void deletingTheStoryDeletesItsHistory() {
        givenAStory("DEL");
        activity.save(at(Instant.now(), null, userId, ChangeSource.USER, ActivityType.STORY_CREATED, null, "Story"));
        entityManager.flush();

        jdbc.update("UPDATE story_activity SET actor_id = NULL WHERE story_id = ?", story.getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM story_activity WHERE story_id = ? AND actor_id IS NULL", Integer.class, story.getId())).isEqualTo(1);

        jdbc.update("DELETE FROM user_stories WHERE id = ?", story.getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM story_activity WHERE story_id = ?", Integer.class, story.getId())).isZero();
    }
}
