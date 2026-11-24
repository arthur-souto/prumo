package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.TokenCipherPort;
import io.github.arthursouto.prumo.application.port.out.result.PullRequestPage;
import io.github.arthursouto.prumo.application.port.out.result.RemotePullRequest;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pull requests that cite a subtask key: where they land and what automation does with the subtask and its story. */
@ExtendWith(MockitoExtension.class)
class SubtaskPullRequestSyncTest {

    @Mock
    private GithubConnectionRepositoryPort connectionRepository;

    @Mock
    private TokenCipherPort cipher;

    @Mock
    private GithubPort github;

    @Mock
    private StoryRepositoryPort storyRepository;

    @Mock
    private SubtaskRepositoryPort subtaskRepository;

    @Mock
    private BoardRepositoryPort boardRepository;

    @Mock
    private ConnectedRepositoryPort repositoryPort;

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final UUID userId = UUID.randomUUID();
    private Workspace workspace;
    private Board board;
    private ConnectedRepository repository;
    private UserStory story;
    private Subtask subtask;
    private Subtask sibling;
    private PullRequestSyncer syncer;
    private final List<PullRequest> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWith(userId);
        board = BoardFixtures.boardFor(workspace);
        repository = ConnectedRepository.connect(workspace.getId(), "arthur", "prumo", "main", "url", userId);
        story = BoardFixtures.story(workspace, board.first(), 12, "V");
        subtask = Subtask.create(story.getId(), workspace.getId(), 13, "PRU-13", "Endpoint", null, 0);
        sibling = Subtask.create(story.getId(), workspace.getId(), 14, "PRU-14", "Tests", null, 1);
        syncer = new PullRequestSyncer(
                connectionRepository, cipher, github, storyRepository, subtaskRepository,
                new SubtaskWorkflow(subtaskRepository, storyRepository), boardRepository, repositoryPort,
                Clock.fixed(now, ZoneOffset.UTC)
        );
        lenient().when(connectionRepository.findByUserId(userId))
                .thenReturn(Optional.of(new GithubConnection(userId, "cipher", "arthur", now)));
        lenient().when(cipher.decrypt("cipher")).thenReturn("ghp_plain");
        lenient().when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        lenient().when(storyRepository.findAllByWorkspaceIdAndKeys(eq(workspace.getId()), any())).thenReturn(List.of());
        lenient().when(storyRepository.findById(story.getId())).thenReturn(Optional.of(story));
        lenient().when(storyRepository.findAllByColumnId(any())).thenReturn(List.of());
        lenient().when(storyRepository.save(any(UserStory.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(storyRepository.findPullRequest(any(), any(), anyInt())).thenReturn(Optional.empty());
        lenient().when(storyRepository.savePullRequest(any())).thenAnswer(call -> {
            saved.add(call.getArgument(0));
            return call.getArgument(0);
        });
        lenient().when(subtaskRepository.findAllByWorkspaceIdAndKeys(eq(workspace.getId()), any())).thenAnswer(call -> {
            Set<String> keys = call.getArgument(1);
            return List.of(subtask, sibling).stream().filter(candidate -> keys.contains(candidate.getKey())).toList();
        });
        lenient().when(subtaskRepository.findAllByStoryId(story.getId())).thenReturn(List.of(subtask, sibling));
        lenient().when(subtaskRepository.save(any(Subtask.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void page(PullRequestState state, String branch, String title) {
        RemotePullRequest remote = new RemotePullRequest(7, title, state, "https://github.com/arthur/prumo/pull/7", branch, now);
        when(github.listPullRequests("ghp_plain", "arthur", "prumo", null))
                .thenReturn(new PullRequestPage(false, "\"etag\"", List.of(remote)));
    }

    @Test
    void aSubtaskKeyInTheBranchPutsThePullRequestOnTheSubtaskAndItsStory() {
        page(PullRequestState.OPEN, "feature/PRU-13-endpoint", "Add endpoint");

        syncer.sync(repository);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).storyId()).isEqualTo(story.getId());
        assertThat(saved.get(0).subtaskId()).isEqualTo(subtask.getId());
    }

    @Test
    void aSubtaskKeyInTheTitleCountsToo() {
        page(PullRequestState.OPEN, "hotfix", "PRU-13: fix the endpoint");

        syncer.sync(repository);

        verify(subtaskRepository).findAllByWorkspaceIdAndKeys(workspace.getId(), Set.of("PRU-13"));
        assertThat(saved).extracting(PullRequest::subtaskId).containsExactly(subtask.getId());
    }

    @Test
    void aStoryKeyAloneKeepsThePullRequestOnTheStory() {
        when(storyRepository.findAllByWorkspaceIdAndKeys(eq(workspace.getId()), any())).thenReturn(List.of(story));
        page(PullRequestState.OPEN, "feature/PRU-12-reset", "Reset");

        syncer.sync(repository);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).subtaskId()).isNull();
        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.TO_DO);
    }

    @Test
    void citingBothTheStoryAndItsSubtaskStoresOnePullRequestOnTheSubtask() {
        when(storyRepository.findAllByWorkspaceIdAndKeys(eq(workspace.getId()), any())).thenReturn(List.of(story));
        board.setAutomationEnabled(true);
        page(PullRequestState.OPEN, "feature/PRU-13-endpoint", "PRU-12 endpoint");

        syncer.sync(repository);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).subtaskId()).isEqualTo(subtask.getId());
        // the subtask moved, the story did not: story automation only applies to pull requests without a subtask key
        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.IN_REVIEW);
        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
    }

    @Test
    void withAutomationOffTheSubtaskKeepsItsStatus() {
        page(PullRequestState.MERGED, "feature/PRU-13-endpoint", "Add endpoint");

        syncer.sync(repository);

        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.TO_DO);
        verify(subtaskRepository, never()).save(any(Subtask.class));
    }

    @Test
    void anOpenPullRequestMovesTheSubtaskToInReviewWhenAutomationIsOn() {
        board.setAutomationEnabled(true);
        page(PullRequestState.OPEN, "feature/PRU-13-endpoint", "Add endpoint");

        syncer.sync(repository);

        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.IN_REVIEW);
        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
    }

    @Test
    void aMergedPullRequestMovesTheSubtaskToDoneButNotTheStoryWhileOthersAreOpen() {
        board.setAutomationEnabled(true);
        board.setConsolidateStories(true);
        page(PullRequestState.MERGED, "feature/PRU-13-endpoint", "Add endpoint");

        syncer.sync(repository);

        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.DONE);
        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
    }

    @Test
    void theLastMergedSubtaskConsolidatesTheStoryIntoDone() {
        board.setAutomationEnabled(true);
        board.setConsolidateStories(true);
        sibling.changeStatus(SubtaskStatus.DONE);
        page(PullRequestState.MERGED, "feature/PRU-13-endpoint", "Add endpoint");

        syncer.sync(repository);

        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.DONE);
        assertThat(story.getColumnId()).isEqualTo(board.ofKind(ColumnKind.DONE).orElseThrow().getId());
    }

    @Test
    void consolidationOffLeavesTheStoryEvenWhenEverySubtaskIsDone() {
        board.setAutomationEnabled(true);
        sibling.changeStatus(SubtaskStatus.DONE);
        page(PullRequestState.MERGED, "feature/PRU-13-endpoint", "Add endpoint");

        syncer.sync(repository);

        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.DONE);
        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
    }

    @Test
    void aClosedPullRequestLeavesTheSubtaskAlone() {
        board.setAutomationEnabled(true);
        subtask.changeStatus(SubtaskStatus.IN_REVIEW);
        page(PullRequestState.CLOSED, "feature/PRU-13-endpoint", "Add endpoint");

        syncer.sync(repository);

        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.IN_REVIEW);
    }

    @Test
    void anUnchangedPullRequestDoesNotReapplyAutomationSoManualChangesStand() {
        board.setAutomationEnabled(true);
        PullRequest known = PullRequest.track(story.getId(), repository.getId(), 7, "Add endpoint", PullRequestState.OPEN,
                "https://github.com/arthur/prumo/pull/7", "feature/PRU-13-endpoint", now).forSubtask(subtask.getId());
        when(storyRepository.findPullRequest(story.getId(), repository.getId(), 7)).thenReturn(Optional.of(known));
        subtask.changeStatus(SubtaskStatus.IN_PROGRESS); // the user moved it back by hand
        page(PullRequestState.OPEN, "feature/PRU-13-endpoint", "Add endpoint");

        syncer.sync(repository);

        assertThat(subtask.getStatus()).isEqualTo(SubtaskStatus.IN_PROGRESS);
        assertThat(saved.get(0).subtaskId()).isEqualTo(subtask.getId());
    }
}
