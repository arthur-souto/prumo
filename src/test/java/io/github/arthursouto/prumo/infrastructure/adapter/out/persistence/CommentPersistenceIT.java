package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.comment.Comment;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.BoardPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.CommentPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.StoryPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.UserStorySearchRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.SubtaskPersistenceAdapter;
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

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BoardPersistenceAdapter.class, StoryPersistenceAdapter.class, UserStorySearchRepository.class, SubtaskPersistenceAdapter.class, CommentPersistenceAdapter.class})
class CommentPersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private BoardPersistenceAdapter boards;

    @Autowired
    private StoryPersistenceAdapter stories;

    @Autowired
    private CommentPersistenceAdapter comments;

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

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void storyAndSubtaskCommentsStayApartAndComeBackOldestFirst() throws Exception {
        givenAStory("CMT");
        comments.save(Comment.create(workspaceId, story.getId(), null, userId, "first on the story"));
        Thread.sleep(5);
        comments.save(Comment.create(workspaceId, story.getId(), "CMT-2", userId, "on the subtask"));
        Thread.sleep(5);
        comments.save(Comment.create(workspaceId, story.getId(), null, userId, "second on the story"));
        flush();

        assertThat(comments.findOfStory(story.getId())).extracting(Comment::getBody)
                .containsExactly("first on the story", "second on the story");
        assertThat(comments.findOfSubtask(workspaceId, "CMT-2")).extracting(Comment::getBody).containsExactly("on the subtask");
        assertThat(comments.findOfSubtask(UUID.randomUUID(), "CMT-2")).isEmpty();
    }

    @Test
    void anEditKeepsTheOriginalDateAndAddsTheEditedOne() {
        givenAStory("EDT");
        Comment saved = comments.save(Comment.create(workspaceId, story.getId(), null, userId, "old"));
        flush();

        Comment loaded = comments.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getEditedAt()).isNull();
        loaded.edit("new");
        comments.save(loaded);
        flush();

        Comment reloaded = comments.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getBody()).isEqualTo("new");
        assertThat(reloaded.getEditedAt()).isNotNull();
        // PostgreSQL keeps microseconds and rounds the rest, while Instant.now() has nanoseconds on Linux
        assertThat(reloaded.getCreatedAt()).isCloseTo(saved.getCreatedAt(), within(1, ChronoUnit.MICROS));
    }

    @Test
    void aLongMarkdownBodyIsStoredWhole() {
        givenAStory("LNG");
        String body = "# Title\n\n" + "line of **markdown**\n".repeat(900);
        Comment saved = comments.save(Comment.create(workspaceId, story.getId(), null, userId, body));
        flush();

        assertThat(comments.findById(saved.getId()).orElseThrow().getBody()).isEqualTo(body);
    }

    @Test
    void deletingRemovesOnlyThatCommentAndDeletingTheStoryRemovesAll() {
        givenAStory("DEL");
        Comment keep = comments.save(Comment.create(workspaceId, story.getId(), null, userId, "keep"));
        Comment drop = comments.save(Comment.create(workspaceId, story.getId(), null, userId, "drop"));
        flush();

        comments.delete(drop);
        flush();

        assertThat(comments.findOfStory(story.getId())).extracting(Comment::getId).containsExactly(keep.getId());

        jdbc.update("DELETE FROM user_stories WHERE id = ?", story.getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM story_comments WHERE story_id = ?", Integer.class, story.getId())).isZero();
    }

    @Test
    void deletingTheAuthorAccountKeepsTheCommentWithoutAnAuthor() {
        givenAStory("USR");
        UUID author = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'A', ?, 'USER', true, now(), now())",
                author, author + "@test.dev");
        Comment saved = comments.save(Comment.create(workspaceId, story.getId(), null, author, "still here"));
        flush();

        jdbc.update("DELETE FROM users WHERE id = ?", author);
        flush();

        Comment loaded = comments.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getAuthorId()).isNull();
        assertThat(loaded.getBody()).isEqualTo("still here");
    }
}
