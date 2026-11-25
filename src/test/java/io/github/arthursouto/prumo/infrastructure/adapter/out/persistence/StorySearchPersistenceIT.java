package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.application.port.out.query.SortDirection;
import io.github.arthursouto.prumo.application.port.out.query.StorySearchCriteria;
import io.github.arthursouto.prumo.application.port.out.query.StorySort;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.BoardPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.StoryPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.UserStorySearchRepository;
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BoardPersistenceAdapter.class, StoryPersistenceAdapter.class, UserStorySearchRepository.class})
class StorySearchPersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private BoardPersistenceAdapter boards;

    @Autowired
    private StoryPersistenceAdapter stories;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID workspaceId;
    private Board board;
    private UUID member;

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'U', ?, 'USER', true, now(), now())",
                id, id + "@test.dev");
        return id;
    }

    private UUID workspace() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO workspaces (id, name, slug, owner_id, created_at, updated_at) VALUES (?, 'W', ?, ?, now(), now())",
                id, "w-" + id, user());
        return id;
    }

    @BeforeEach
    void setUp() {
        workspaceId = workspace();
        member = user();
        board = boards.save(Board.withDefaults(workspaceId, "S" + UUID.randomUUID().toString().substring(0, 6).toUpperCase()));
    }

    private UserStory story(int number, String title, String description, StoryPriority priority, ColumnKind kind, String... labels) {
        UserStory story = UserStory.create(
                workspaceId, number, board.getKeyPrefix() + "-" + number, title, description,
                priority, null, board.ofKind(kind).orElseThrow().getId(), "r" + number,
                List.of(labels), List.of()
        );
        return stories.save(story);
    }

    private StorySearchCriteria criteria() {
        return new StorySearchCriteria(workspaceId, null, Set.of(), Set.of(), Set.of(), Set.of(), false, Set.of(), false,
                StorySort.NUMBER, SortDirection.DESC, 0, 20);
    }

    private static List<Integer> numbers(PageResult<UserStory> page) {
        return page.items().stream().map(UserStory::getNumber).toList();
    }

    @Test
    void listsOnlyTheWorkspaceStoriesNewestFirstByDefault() {
        story(1, "First", null, StoryPriority.LOW, ColumnKind.TODO);
        story(2, "Second", null, StoryPriority.LOW, ColumnKind.TODO);
        UUID otherWorkspace = workspace();
        boards.save(Board.withDefaults(otherWorkspace, "O" + UUID.randomUUID().toString().substring(0, 6).toUpperCase()));

        PageResult<UserStory> page = stories.search(criteria());

        assertThat(numbers(page)).containsExactly(2, 1);
        assertThat(page.totalItems()).isEqualTo(2);
    }

    @Test
    void searchesTheTitleKeyAndDescriptionIgnoringCase() {
        story(1, "Login page", null, StoryPriority.LOW, ColumnKind.TODO);
        story(2, "Reset password", "Uses the OAuth flow", StoryPriority.LOW, ColumnKind.TODO);
        story(3, "Other", null, StoryPriority.LOW, ColumnKind.TODO);
        String key = board.getKeyPrefix().toLowerCase() + "-3";

        assertThat(numbers(stories.search(with(criteria(), "login")))).containsExactly(1);
        assertThat(numbers(stories.search(with(criteria(), "oauth")))).containsExactly(2);
        assertThat(numbers(stories.search(with(criteria(), key)))).containsExactly(3);
    }

    @Test
    void searchesIgnoringAccentsOnBothSides() {
        story(1, "Ação de exportação", null, StoryPriority.LOW, ColumnKind.TODO);
        story(2, "Relatorio", "Gera o relatório de CONFIGURAÇÕES", StoryPriority.LOW, ColumnKind.TODO);
        story(3, "Other", null, StoryPriority.LOW, ColumnKind.TODO);

        assertThat(numbers(stories.search(with(criteria(), "acao")))).containsExactly(1);
        assertThat(numbers(stories.search(with(criteria(), "exportação")))).containsExactly(1);
        assertThat(numbers(stories.search(with(criteria(), "relatório")))).containsExactly(2);
        assertThat(numbers(stories.search(with(criteria(), "configuracoes")))).containsExactly(2);
    }

    @Test
    void treatsLikeWildcardsInTheTextLiterally() {
        story(1, "Coverage 100%", null, StoryPriority.LOW, ColumnKind.TODO);
        story(2, "Coverage 1000", null, StoryPriority.LOW, ColumnKind.TODO);
        story(3, "snake_case", null, StoryPriority.LOW, ColumnKind.TODO);
        story(4, "snakeXcase", null, StoryPriority.LOW, ColumnKind.TODO);

        assertThat(numbers(stories.search(with(criteria(), "100%")))).containsExactly(1);
        assertThat(numbers(stories.search(with(criteria(), "snake_")))).containsExactly(3);
    }

    @Test
    void filtersByAnyOfTheLabelsIgnoringCase() {
        story(1, "A", null, StoryPriority.LOW, ColumnKind.TODO, "Frontend", "api");
        story(2, "B", null, StoryPriority.LOW, ColumnKind.TODO, "backend");
        story(3, "C", null, StoryPriority.LOW, ColumnKind.TODO);

        StorySearchCriteria criteria = criteria();
        PageResult<UserStory> page = stories.search(new StorySearchCriteria(
                workspaceId, null, Set.of("frontend", "backend"), Set.of(), Set.of(), Set.of(), false, Set.of(), false,
                criteria.sort(), criteria.direction(), 0, 20
        ));

        assertThat(numbers(page)).containsExactly(2, 1);
    }

    @Test
    void combinesPriorityColumnAndAssigneeFilters() {
        UserStory assigned = story(1, "A", null, StoryPriority.HIGH, ColumnKind.TODO);
        stories.save(assigned.withAssignee(member));
        story(2, "B", null, StoryPriority.HIGH, ColumnKind.TODO);
        story(3, "C", null, StoryPriority.URGENT, ColumnKind.DONE);
        story(4, "D", null, StoryPriority.LOW, ColumnKind.TODO);
        UUID todo = board.ofKind(ColumnKind.TODO).orElseThrow().getId();
        UUID done = board.ofKind(ColumnKind.DONE).orElseThrow().getId();

        assertThat(numbers(stories.search(new StorySearchCriteria(
                workspaceId, null, Set.of(), Set.of(StoryPriority.HIGH, StoryPriority.URGENT), Set.of(todo), Set.of(), false, Set.of(), false,
                StorySort.NUMBER, SortDirection.DESC, 0, 20
        )))).containsExactly(2, 1);
        assertThat(numbers(stories.search(new StorySearchCriteria(
                workspaceId, null, Set.of(), Set.of(), Set.of(done), Set.of(), false, Set.of(), false,
                StorySort.NUMBER, SortDirection.DESC, 0, 20
        )))).containsExactly(3);
        assertThat(numbers(stories.search(new StorySearchCriteria(
                workspaceId, null, Set.of(), Set.of(), Set.of(), Set.of(member), false, Set.of(), false,
                StorySort.NUMBER, SortDirection.DESC, 0, 20
        )))).containsExactly(1);
        assertThat(numbers(stories.search(new StorySearchCriteria(
                workspaceId, null, Set.of(), Set.of(StoryPriority.HIGH), Set.of(), Set.of(member), true, Set.of(), false,
                StorySort.NUMBER, SortDirection.DESC, 0, 20
        )))).containsExactly(2, 1);
    }

    @Test
    void sortsByPriorityThenByNumber() {
        story(1, "A", null, StoryPriority.MEDIUM, ColumnKind.TODO);
        story(2, "B", null, StoryPriority.URGENT, ColumnKind.TODO);
        story(3, "C", null, StoryPriority.LOW, ColumnKind.TODO);
        story(4, "D", null, StoryPriority.URGENT, ColumnKind.TODO);

        assertThat(numbers(stories.search(sorted(StorySort.PRIORITY, SortDirection.DESC)))).containsExactly(4, 2, 1, 3);
        assertThat(numbers(stories.search(sorted(StorySort.PRIORITY, SortDirection.ASC)))).containsExactly(3, 1, 2, 4);
    }

    @Test
    void sortsByLastUpdate() {
        story(1, "A", null, StoryPriority.LOW, ColumnKind.TODO);
        story(2, "B", null, StoryPriority.LOW, ColumnKind.TODO);
        story(3, "C", null, StoryPriority.LOW, ColumnKind.TODO);
        jdbc.update("UPDATE user_stories SET updated_at = now() + interval '1 hour' WHERE workspace_id = ? AND story_number = 1", workspaceId);
        jdbc.update("UPDATE user_stories SET updated_at = now() - interval '1 hour' WHERE workspace_id = ? AND story_number = 3", workspaceId);

        assertThat(numbers(stories.search(sorted(StorySort.UPDATED_AT, SortDirection.DESC)))).containsExactly(1, 2, 3);
    }

    @Test
    void paginatesAndCountsTheMatchingStories() {
        for (int number = 1; number <= 5; number++) {
            story(number, "Story " + number, null, StoryPriority.LOW, ColumnKind.TODO, "api");
        }

        PageResult<UserStory> first = stories.search(page(0, 2));
        PageResult<UserStory> last = stories.search(page(2, 2));
        PageResult<UserStory> beyond = stories.search(page(5, 2));

        assertThat(numbers(first)).containsExactly(5, 4);
        assertThat(first.totalItems()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.items().get(0).getLabels()).containsExactly("api");
        assertThat(numbers(last)).containsExactly(1);
        assertThat(last.totalItems()).isEqualTo(5);
        assertThat(beyond.items()).isEmpty();
        assertThat(beyond.totalItems()).isEqualTo(5);
    }

    @Test
    void filtersBySprintAndByTheBacklog() {
        UUID sprint = UUID.randomUUID();
        jdbc.update("INSERT INTO sprints (id, workspace_id, name, start_date, end_date, state, created_at, updated_at) "
                + "VALUES (?, ?, 'S', current_date, current_date, 'PLANNED', now(), now())", sprint, workspaceId);
        story(1, "A", null, StoryPriority.LOW, ColumnKind.TODO);
        story(2, "B", null, StoryPriority.LOW, ColumnKind.TODO);
        story(3, "C", null, StoryPriority.LOW, ColumnKind.TODO);
        jdbc.update("UPDATE user_stories SET sprint_id = ? WHERE workspace_id = ? AND story_number IN (1, 2)", sprint, workspaceId);

        assertThat(numbers(stories.search(inSprint(Set.of(sprint), false)))).containsExactly(2, 1);
        assertThat(numbers(stories.search(inSprint(Set.of(), true)))).containsExactly(3);
        assertThat(numbers(stories.search(inSprint(Set.of(sprint), true)))).containsExactly(3, 2, 1);
        assertThat(numbers(stories.search(inSprint(Set.of(UUID.randomUUID()), false)))).isEmpty();
    }

    @Test
    void theTextSearchCanUseTheTrigramIndexes() {
        List<String> plan = jdbc.execute((Connection connection) -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL enable_seqscan = off");
                ResultSet result = statement.executeQuery("""
                        EXPLAIN SELECT id FROM user_stories
                        WHERE immutable_unaccent(lower(title)) LIKE immutable_unaccent('%login%')
                           OR lower(story_key) LIKE '%login%'
                           OR immutable_unaccent(lower(description)) LIKE immutable_unaccent('%login%')
                        """);
                List<String> lines = new ArrayList<>();
                while (result.next()) {
                    lines.add(result.getString(1));
                }
                return lines;
            }
        });

        assertThat(String.join("\n", plan))
                .contains("idx_user_stories_title_trgm")
                .contains("idx_user_stories_key_trgm")
                .contains("idx_user_stories_description_trgm");
    }

    private StorySearchCriteria with(StorySearchCriteria base, String text) {
        return new StorySearchCriteria(base.workspaceId(), text, base.labels(), base.priorities(), base.columnIds(),
                base.assigneeIds(), base.unassigned(), base.sprintIds(), base.withoutSprint(), base.sort(), base.direction(),
                base.page(), base.size());
    }

    private StorySearchCriteria inSprint(Set<UUID> sprintIds, boolean withoutSprint) {
        return new StorySearchCriteria(workspaceId, null, Set.of(), Set.of(), Set.of(), Set.of(), false, sprintIds, withoutSprint,
                StorySort.NUMBER, SortDirection.DESC, 0, 20);
    }

    private StorySearchCriteria sorted(StorySort sort, SortDirection direction) {
        return new StorySearchCriteria(workspaceId, null, Set.of(), Set.of(), Set.of(), Set.of(), false, Set.of(), false, sort, direction, 0, 20);
    }

    private StorySearchCriteria page(int page, int size) {
        return new StorySearchCriteria(workspaceId, null, Set.of(), Set.of(), Set.of(), Set.of(), false, Set.of(), false,
                StorySort.NUMBER, SortDirection.DESC, page, size);
    }
}
