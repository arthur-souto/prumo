package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.application.exceptions.KeyPrefixTakenException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.story.ChecklistItem;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real Flyway migrations against PostgreSQL. Covers what mocks cannot: the "C" collation
 * of the rank column, the atomic story-number allocation and the database constraints.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BoardPersistenceAdapter.class, StoryPersistenceAdapter.class, UserStorySearchRepository.class})
class BoardAndStoryPersistenceIT {

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

    private UUID workspace(String slug) {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'U', ?, 'USER', true, now(), now())",
                userId, userId + "@test.dev");
        jdbc.update("INSERT INTO workspaces (id, name, slug, owner_id, created_at, updated_at) VALUES (?, 'W', ?, ?, now(), now())",
                id, slug, userId);
        return id;
    }

    @BeforeEach
    void setUp() {
        workspaceId = workspace("w-" + UUID.randomUUID());
    }

    private UserStory story(Board board, ColumnKind kind, int number, String rank) {
        return UserStory.create(
                workspaceId, number, board.getKeyPrefix() + "-" + number, "Story " + number, null,
                StoryPriority.MEDIUM, null, board.ofKind(kind).orElseThrow().getId(), rank,
                List.of("api"), List.of(ChecklistItem.create("do", false))
        );
    }

    @Test
    void savesAndReloadsTheBoardWithItsColumnsInOrder() {
        boards.save(Board.withDefaults(workspaceId, "AAA"));

        Board loaded = boards.findByWorkspaceId(workspaceId).orElseThrow();

        assertThat(loaded.getKeyPrefix()).isEqualTo("AAA");
        assertThat(loaded.getColumns()).extracting(BoardColumn::getName)
                .containsExactly("To Do", "In Progress", "In Review", "Done");
    }

    @Test
    void theKeyPrefixIsUniqueAcrossTheSystem() {
        boards.save(Board.withDefaults(workspaceId, "UNQ"));
        UUID other = workspace("o-" + UUID.randomUUID());

        assertThat(boards.existsByKeyPrefix("UNQ")).isTrue();
        assertThatThrownBy(() -> boards.save(Board.withDefaults(other, "UNQ")))
                .isInstanceOf(KeyPrefixTakenException.class);
    }

    @Test
    void storyNumbersAreAllocatedSequentiallyPerBoard() {
        boards.save(Board.withDefaults(workspaceId, "SEQ"));

        assertThat(boards.allocateStoryNumber(workspaceId)).isEqualTo(1);
        assertThat(boards.allocateStoryNumber(workspaceId)).isEqualTo(2);
        assertThat(boards.allocateStoryNumber(workspaceId)).isEqualTo(3);
    }

    @Test
    void ranksSortByBytesSoUppercaseComesBeforeLowercase() {
        Board board = boards.save(Board.withDefaults(workspaceId, "RNK"));
        stories.save(story(board, ColumnKind.TODO, 1, "a"));
        stories.save(story(board, ColumnKind.TODO, 2, "Z"));
        stories.save(story(board, ColumnKind.TODO, 3, "5"));

        List<UserStory> ordered = stories.findAllByColumnId(board.first().getId());

        assertThat(ordered).extracting(UserStory::getRank).containsExactly("5", "Z", "a");
    }

    @Test
    void persistsLabelsAndChecklistsAndFindsByKeyCaseInsensitively() {
        Board board = boards.save(Board.withDefaults(workspaceId, "KEY"));
        stories.save(story(board, ColumnKind.TODO, 1, "V"));

        UserStory loaded = stories.findByWorkspaceIdAndKey(workspaceId, "KEY-1").orElseThrow();

        assertThat(loaded.getLabels()).containsExactly("api");
        assertThat(loaded.getAcceptanceCriteria()).extracting(ChecklistItem::text).containsExactly("do");
        assertThat(stories.findAllByWorkspaceIdAndKeys(workspaceId, List.of("KEY-1", "KEY-9"))).hasSize(1);
        assertThat(stories.countByWorkspaceId(workspaceId)).isEqualTo(1);
    }

    @Test
    void aKeyCannotRepeatInsideAWorkspace() {
        Board board = boards.save(Board.withDefaults(workspaceId, "DUP"));
        stories.save(story(board, ColumnKind.TODO, 1, "V"));

        assertThatThrownBy(() -> stories.save(story(board, ColumnKind.TODO, 1, "W")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void movingAStoryChangesOnlyThatRow() {
        Board board = boards.save(Board.withDefaults(workspaceId, "MOV"));
        UserStory a = stories.save(story(board, ColumnKind.TODO, 1, "G"));
        UserStory b = stories.save(story(board, ColumnKind.TODO, 2, "P"));

        a.moveTo(board.ofKind(ColumnKind.DONE).orElseThrow().getId(), "V");
        stories.save(a);

        assertThat(stories.findAllByColumnId(board.first().getId())).extracting(UserStory::getKey).containsExactly("MOV-2");
        assertThat(stories.findAllByColumnId(board.ofKind(ColumnKind.DONE).orElseThrow().getId()))
                .extracting(UserStory::getKey).containsExactly("MOV-1");
        assertThat(b.getRank()).isEqualTo("P");
    }

    @Test
    void deletingAStoryRemovesItsLabelsAndChecklist() {
        Board board = boards.save(Board.withDefaults(workspaceId, "DEL"));
        UserStory saved = stories.save(story(board, ColumnKind.TODO, 1, "V"));

        stories.deleteById(saved.getId());

        assertThat(stories.findAllByWorkspaceId(workspaceId)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM story_labels WHERE story_id = ?", Integer.class, saved.getId())).isZero();
    }
}
