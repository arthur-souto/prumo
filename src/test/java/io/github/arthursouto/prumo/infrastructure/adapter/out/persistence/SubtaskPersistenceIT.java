package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.SubtaskProgress;
import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.BoardPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.StoryPersistenceAdapter;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BoardPersistenceAdapter.class, StoryPersistenceAdapter.class, SubtaskPersistenceAdapter.class})
class SubtaskPersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private BoardPersistenceAdapter boards;

    @Autowired
    private StoryPersistenceAdapter stories;

    @Autowired
    private SubtaskPersistenceAdapter subtasks;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private UUID workspaceId;
    private Board board;
    private UserStory story;

    private void givenAStory(String prefix) {
        UUID userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'U', ?, 'USER', true, now(), now())",
                userId, userId + "@test.dev");
        jdbc.update("INSERT INTO workspaces (id, name, slug, owner_id, created_at, updated_at) VALUES (?, 'W', ?, ?, now(), now())",
                workspaceId, "w-" + workspaceId, userId);
        board = boards.save(Board.withDefaults(workspaceId, prefix));
        int number = boards.allocateStoryNumber(workspaceId);
        story = stories.save(UserStory.create(workspaceId, number, prefix + "-" + number, "Story", null,
                StoryPriority.MEDIUM, null, board.ofKind(ColumnKind.TODO).orElseThrow().getId(), "V", List.of(), List.of()));
    }

    private Subtask newSubtask(String prefix, String title, int position) {
        int number = boards.allocateStoryNumber(workspaceId);

        return Subtask.create(story.getId(), workspaceId, number, prefix + "-" + number, title, null, position);
    }

    @Test
    void storiesAndSubtasksShareOneNumberSequenceSoKeysNeverCollide() {
        givenAStory("SHR");

        Subtask first = subtasks.save(newSubtask("SHR", "One", 0));
        Subtask second = subtasks.save(newSubtask("SHR", "Two", 1));

        assertThat(story.getKey()).isEqualTo("SHR-1");
        assertThat(first.getKey()).isEqualTo("SHR-2");
        assertThat(second.getKey()).isEqualTo("SHR-3");
        assertThat(boards.allocateStoryNumber(workspaceId)).isEqualTo(4);
    }

    @Test
    void savesAndFindsSubtasksByKeyAndByStoryInOrder() {
        givenAStory("FND");
        Subtask second = newSubtask("FND", "Second", 1);
        Subtask first = newSubtask("FND", "First", 0);
        first.recordBranch("feature/FND-3-first");
        subtasks.save(second);
        subtasks.save(first);
        first.changeStatus(SubtaskStatus.IN_REVIEW);
        subtasks.save(first);

        assertThat(subtasks.findAllByStoryId(story.getId())).extracting(Subtask::getTitle).containsExactly("First", "Second");
        Subtask loaded = subtasks.findByWorkspaceIdAndKey(workspaceId, first.getKey()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(SubtaskStatus.IN_REVIEW);
        assertThat(loaded.getBranchName()).isEqualTo("feature/FND-3-first");
        assertThat(subtasks.findAllByWorkspaceIdAndKeys(workspaceId, List.of(first.getKey(), "FND-99"))).hasSize(1);
    }

    @Test
    void aSubtaskKeyCannotRepeatInsideAWorkspace() {
        givenAStory("DUP");
        Subtask original = subtasks.save(newSubtask("DUP", "One", 0));

        Subtask clash = Subtask.create(story.getId(), workspaceId, 99, original.getKey(), "Clash", null, 1);

        assertThatThrownBy(() -> subtasks.save(clash)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deletingTheStoryRemovesItsSubtasksButDeletingASubtaskKeepsItsPullRequestOnTheStory() {
        givenAStory("DEL");
        Subtask subtask = subtasks.save(newSubtask("DEL", "One", 0));
        UUID repositoryId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'U', ?, 'USER', true, now(), now())",
                repositoryId, repositoryId + "@test.dev");
        jdbc.update("INSERT INTO connected_repositories (id, workspace_id, owner, name, default_branch, html_url, connected_by, created_at) VALUES (?, ?, 'o', 'r', 'main', 'https://github.com/o/r', ?, now())",
                repositoryId, workspaceId, repositoryId);
        stories.savePullRequest(PullRequest.track(story.getId(), repositoryId, 5, "PR", PullRequestState.OPEN,
                "https://github.com/o/r/pull/5", "feature/x", Instant.now()).forSubtask(subtask.getId()));

        subtasks.deleteById(subtask.getId());

        // push the writes to PostgreSQL and drop the JPA cache, so the check sees the database's ON DELETE SET NULL
        entityManager.flush();
        entityManager.clear();
        PullRequest kept = stories.findPullRequest(story.getId(), repositoryId, 5).orElseThrow();
        assertThat(kept.subtaskId()).isNull();

        Subtask another = subtasks.save(newSubtask("DEL", "Two", 0));
        stories.deleteById(story.getId());
        entityManager.flush();
        entityManager.clear();
        assertThat(subtasks.findByWorkspaceIdAndKey(workspaceId, another.getKey())).isEmpty();
    }

    @Test
    void reportsTheProgressOfEachStoryAndPullRequestsKeepTheirSubtask() {
        givenAStory("PRG");
        Subtask done = newSubtask("PRG", "One", 0);
        done.changeStatus(SubtaskStatus.DONE);
        subtasks.save(done);
        Subtask open = subtasks.save(newSubtask("PRG", "Two", 1));
        UserStory empty = stories.save(UserStory.create(workspaceId, boards.allocateStoryNumber(workspaceId), "PRG-9", "Empty", null,
                StoryPriority.MEDIUM, null, board.first().getId(), "W", List.of(), List.of()));

        assertThat(subtasks.progressByStoryIds(List.of(story.getId(), empty.getId())))
                .containsOnlyKeys(story.getId())
                .containsEntry(story.getId(), new SubtaskProgress(2, 1));
        assertThat(subtasks.progressByStoryIds(List.of())).isEmpty();

        UUID repositoryId = UUID.randomUUID();
        UUID connector = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'U', ?, 'USER', true, now(), now())",
                connector, connector + "@test.dev");
        jdbc.update("INSERT INTO connected_repositories (id, workspace_id, owner, name, default_branch, html_url, connected_by, created_at) VALUES (?, ?, 'o', 'r', 'main', 'https://github.com/o/r', ?, now())",
                repositoryId, workspaceId, connector);
        stories.savePullRequest(PullRequest.track(story.getId(), repositoryId, 3, "PR", PullRequestState.OPEN,
                "https://github.com/o/r/pull/3", "feature/x", Instant.now()).forSubtask(open.getId()));
        entityManager.flush();
        entityManager.clear();

        assertThat(stories.findByWorkspaceIdAndKey(workspaceId, story.getKey()).orElseThrow().getPullRequests())
                .extracting(PullRequest::subtaskId).containsExactly(open.getId());
    }
}
