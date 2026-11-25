package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.application.port.out.query.SprintSearchCriteria;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintState;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.BoardPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.SprintPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.StoryPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.UserStorySearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BoardPersistenceAdapter.class, StoryPersistenceAdapter.class, SprintPersistenceAdapter.class, UserStorySearchRepository.class})
class SprintPersistenceIT {

    private static final LocalDate START = LocalDate.of(2026, 12, 1);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private BoardPersistenceAdapter boards;

    @Autowired
    private StoryPersistenceAdapter stories;

    @Autowired
    private SprintPersistenceAdapter sprints;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID workspaceId;
    private Board board;

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
        workspaceId = workspace();
        board = boards.save(Board.withDefaults(workspaceId, "S" + UUID.randomUUID().toString().substring(0, 6).toUpperCase()));
    }

    private Sprint sprint(String name, LocalDate start) {
        return sprints.save(Sprint.create(workspaceId, name, "Goal", start, start.plusDays(13)));
    }

    private UserStory story(int number, Integer points, UUID sprintId) {
        UserStory story = UserStory.create(workspaceId, number, board.getKeyPrefix() + "-" + number, "Story", null,
                StoryPriority.MEDIUM, points, board.ofKind(ColumnKind.TODO).orElseThrow().getId(), "r" + number, List.of(), List.of());
        return stories.save(story.withSprint(sprintId));
    }

    @Test
    void savesAndRestoresTheWholeLifecycle() {
        Sprint sprint = sprint("Sprint 1", START);
        sprint.start(8);
        sprints.save(sprint);
        sprint.close(5);
        sprints.save(sprint);

        Sprint restored = sprints.findById(sprint.getId()).orElseThrow();

        assertThat(restored.getState()).isEqualTo(SprintState.CLOSED);
        assertThat(restored).extracting(Sprint::getName, Sprint::getGoal, Sprint::getStartDate, Sprint::getEndDate,
                        Sprint::getCommittedPoints, Sprint::getCompletedPoints)
                .containsExactly("Sprint 1", "Goal", START, START.plusDays(13), 8, 5);
        assertThat(restored.getStartedAt()).isNotNull();
        assertThat(restored.getClosedAt()).isNotNull();
    }

    private SprintSearchCriteria criteria(String text, Set<SprintState> states, int page, int size) {
        return new SprintSearchCriteria(workspaceId, text, states, page, size);
    }

    private static List<String> names(PageResult<Sprint> page) {
        return page.items().stream().map(Sprint::getName).toList();
    }

    @Test
    void searchesTheActiveFirstThenPlannedByStartThenClosedLatestFirst() {
        Sprint oldest = sprint("Oldest", START.minusDays(28));
        oldest.start(0);
        sprints.save(oldest);
        oldest.close(0);
        sprints.save(oldest);
        Sprint old = sprint("Old", START.minusDays(14));
        old.start(0);
        sprints.save(old);
        old.close(0);
        sprints.save(old);
        sprint("Later", START.plusDays(28));
        sprint("Next", START.plusDays(14));
        Sprint now = sprint("Now", START);
        now.start(0);
        sprints.save(now);

        assertThat(sprints.findActive(workspaceId)).map(Sprint::getId).contains(now.getId());
        assertThat(names(sprints.search(criteria(null, EnumSet.allOf(SprintState.class), 0, 20))))
                .containsExactly("Now", "Next", "Later", "Old", "Oldest");
        assertThat(names(sprints.search(criteria(null, EnumSet.of(SprintState.PLANNED, SprintState.CLOSED), 0, 20))))
                .containsExactly("Next", "Later", "Old", "Oldest");
    }

    @Test
    void searchesNameAndGoalIgnoringCaseAndAccents() {
        sprint("Integração", START);
        Sprint withGoal = sprints.save(Sprint.create(workspaceId, "Sprint 2", "Lançar o BOARD", START.plusDays(14), START.plusDays(27)));
        sprint("100% pronto", START.plusDays(28));

        assertThat(names(sprints.search(criteria("integracao", EnumSet.allOf(SprintState.class), 0, 20)))).containsExactly("Integração");
        assertThat(names(sprints.search(criteria("lancar o board", EnumSet.allOf(SprintState.class), 0, 20)))).containsExactly(withGoal.getName());
        assertThat(names(sprints.search(criteria("100%", EnumSet.allOf(SprintState.class), 0, 20)))).containsExactly("100% pronto");
        assertThat(names(sprints.search(criteria("0%p", EnumSet.allOf(SprintState.class), 0, 20)))).isEmpty();
    }

    @Test
    void paginatesAndCountsTheMatchingSprints() {
        for (int index = 0; index < 5; index++) {
            sprint("Sprint " + index, START.plusDays(14L * index));
        }
        sprints.save(Sprint.create(workspace(), "Elsewhere", null, START, START));

        PageResult<Sprint> second = sprints.search(criteria(null, EnumSet.allOf(SprintState.class), 1, 2));

        assertThat(names(second)).containsExactly("Sprint 2", "Sprint 3");
        assertThat(second.totalItems()).isEqualTo(5);
        assertThat(second.totalPages()).isEqualTo(3);
    }

    @Test
    void theDatabaseKeepsOnlyOneActiveSprintPerWorkspace() {
        Sprint one = sprint("One", START);
        one.start(0);
        sprints.save(one);
        Sprint two = sprint("Two", START.plusDays(14));
        two.start(0);

        assertThatThrownBy(() -> sprints.save(two)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void storiesKeepTheirSprintAndAreFoundBySprint() {
        Sprint one = sprint("One", START);
        Sprint two = sprint("Two", START.plusDays(14));
        story(1, 3, one.getId());
        story(2, 5, two.getId());
        story(3, 1, null);

        assertThat(stories.findAllBySprintIds(List.of(one.getId(), two.getId())))
                .extracting(UserStory::getNumber, UserStory::getSprintId)
                .containsExactlyInAnyOrder(tuple(1, one.getId()), tuple(2, two.getId()));
        assertThat(stories.findAllBySprintIds(List.of())).isEmpty();
    }

    @Test
    void deletingASprintSendsItsStoriesBackToTheBacklog() {
        Sprint sprint = sprint("One", START);
        UserStory story = story(1, 3, sprint.getId());

        sprints.delete(sprint);

        assertThat(stories.findAllBySprintIds(List.of(sprint.getId()))).isEmpty();
        assertThat(jdbc.queryForObject("SELECT sprint_id FROM user_stories WHERE id = ?", UUID.class, story.getId())).isNull();
    }
}
