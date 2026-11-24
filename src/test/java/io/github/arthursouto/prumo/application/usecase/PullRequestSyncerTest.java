package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.TokenCipherPort;
import io.github.arthursouto.prumo.application.port.out.result.PullRequestPage;
import io.github.arthursouto.prumo.application.port.out.result.RemotePullRequest;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PullRequestSyncerTest {

    @Mock
    private GithubConnectionRepositoryPort connectionRepository;

    @Mock
    private TokenCipherPort cipher;

    @Mock
    private GithubPort github;

    @Mock
    private StoryRepositoryPort storyRepository;

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
    private PullRequestSyncer syncer;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWith(userId);
        board = BoardFixtures.boardFor(workspace);
        repository = ConnectedRepository.connect(workspace.getId(), "arthur", "prumo", "main", "url", userId);
        story = BoardFixtures.story(workspace, board.first(), 12, "V");
        syncer = new PullRequestSyncer(
                connectionRepository,
                cipher,
                github,
                storyRepository,
                boardRepository,
                repositoryPort,
                Clock.fixed(now, ZoneOffset.UTC)
        );
        lenient().when(connectionRepository.findByUserId(userId))
                .thenReturn(Optional.of(new GithubConnection(userId, "cipher", "arthur", now)));
        lenient().when(cipher.decrypt("cipher")).thenReturn("ghp_plain");
        lenient().when(boardRepository.findByWorkspaceId(workspace.getId())).thenReturn(Optional.of(board));
        lenient().when(storyRepository.findAllByWorkspaceIdAndKeys(eq(workspace.getId()), any()))
                .thenReturn(List.of(story));
        lenient().when(storyRepository.findAllByColumnId(any())).thenReturn(List.of());
        lenient().when(storyRepository.findPullRequest(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Optional.empty());
    }

    private RemotePullRequest remote(int number, String title, PullRequestState state, String branch) {
        return new RemotePullRequest(number, title, state, "https://github.com/arthur/prumo/pull/" + number, branch, now);
    }

    private void page(RemotePullRequest... items) {
        when(github.listPullRequests("ghp_plain", "arthur", "prumo", null))
                .thenReturn(new PullRequestPage(false, "\"etag-1\"", List.of(items)));
    }

    @Test
    void aPullRequestWhoseBranchHoldsTheKeyIsLinkedToTheStory() {
        page(remote(7, "Add reset form", PullRequestState.OPEN, "feature/PRU-12-reset"));
        ArgumentCaptor<PullRequest> saved = ArgumentCaptor.forClass(PullRequest.class);
        when(storyRepository.savePullRequest(saved.capture())).thenAnswer(call -> call.getArgument(0));

        syncer.sync(repository);

        assertThat(saved.getValue().storyId()).isEqualTo(story.getId());
        assertThat(saved.getValue().repositoryId()).isEqualTo(repository.getId());
        assertThat(saved.getValue().number()).isEqualTo(7);
        assertThat(saved.getValue().state()).isEqualTo(PullRequestState.OPEN);
        assertThat(saved.getValue().url()).endsWith("/pull/7");
    }

    @Test
    void theKeyInTheTitleAlsoCounts() {
        page(remote(8, "PRU-12: fix", PullRequestState.OPEN, "hotfix"));
        when(storyRepository.savePullRequest(any())).thenAnswer(call -> call.getArgument(0));

        syncer.sync(repository);

        verify(storyRepository).findAllByWorkspaceIdAndKeys(workspace.getId(), Set.of("PRU-12"));
        verify(storyRepository).savePullRequest(any());
    }

    @Test
    void pullRequestsWithoutAnyKeyAreIgnored() {
        page(remote(9, "Update readme", PullRequestState.OPEN, "docs"));

        syncer.sync(repository);

        verify(storyRepository, never()).savePullRequest(any());
        verify(storyRepository, never()).findAllByWorkspaceIdAndKeys(any(), any());
    }

    @Test
    void keysThatMatchNoStoryAreIgnored() {
        page(remote(10, "PRU-99 something", PullRequestState.OPEN, "x"));
        when(storyRepository.findAllByWorkspaceIdAndKeys(eq(workspace.getId()), any())).thenReturn(List.of());

        syncer.sync(repository);

        verify(storyRepository, never()).savePullRequest(any());
    }

    @Test
    void automationIsOffByDefaultSoCardsStayPut() {
        page(remote(7, "x", PullRequestState.MERGED, "PRU-12"));
        when(storyRepository.savePullRequest(any())).thenAnswer(call -> call.getArgument(0));

        syncer.sync(repository);

        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
        verify(storyRepository, never()).save(any());
    }

    @Test
    void anOpenPullRequestMovesTheStoryToInReviewWhenAutomationIsOn() {
        board.setAutomationEnabled(true);
        page(remote(7, "x", PullRequestState.OPEN, "PRU-12"));
        when(storyRepository.savePullRequest(any())).thenAnswer(call -> call.getArgument(0));

        syncer.sync(repository);

        assertThat(story.getColumnId()).isEqualTo(BoardFixtures.column(board, ColumnKind.IN_REVIEW).getId());
        verify(storyRepository).save(story);
    }

    @Test
    void aMergedPullRequestMovesTheStoryToTheEndOfDone() {
        board.setAutomationEnabled(true);
        UUID done = BoardFixtures.column(board, ColumnKind.DONE).getId();
        UserStory finished = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.DONE), 3, "k");
        when(storyRepository.findAllByColumnId(done)).thenReturn(List.of(finished));
        page(remote(7, "x", PullRequestState.MERGED, "PRU-12"));
        when(storyRepository.savePullRequest(any())).thenAnswer(call -> call.getArgument(0));

        syncer.sync(repository);

        assertThat(story.getColumnId()).isEqualTo(done);
        assertThat(story.getRank()).isGreaterThan("k");
    }

    @Test
    void aClosedWithoutMergePullRequestDoesNotMoveAnything() {
        board.setAutomationEnabled(true);
        page(remote(7, "x", PullRequestState.CLOSED, "PRU-12"));
        when(storyRepository.savePullRequest(any())).thenAnswer(call -> call.getArgument(0));

        syncer.sync(repository);

        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
        verify(storyRepository, never()).save(any());
    }

    @Test
    void anUnchangedPullRequestDoesNotFightTheUserWhoMovedTheCardBack() {
        board.setAutomationEnabled(true);
        PullRequest known = PullRequest.track(
                story.getId(), repository.getId(), 7, "x", PullRequestState.OPEN, "https://x/7", "PRU-12", now
        );
        when(storyRepository.findPullRequest(story.getId(), repository.getId(), 7)).thenReturn(Optional.of(known));
        page(remote(7, "x edited", PullRequestState.OPEN, "PRU-12"));
        ArgumentCaptor<PullRequest> saved = ArgumentCaptor.forClass(PullRequest.class);
        when(storyRepository.savePullRequest(saved.capture())).thenAnswer(call -> call.getArgument(0));

        syncer.sync(repository);

        assertThat(saved.getValue().id()).isEqualTo(known.id());
        assertThat(saved.getValue().title()).isEqualTo("x edited");
        assertThat(story.getColumnId()).isEqualTo(board.first().getId());
        verify(storyRepository, never()).save(any());
    }

    @Test
    void aStateChangeOnAKnownPullRequestMovesTheStoryAgain() {
        board.setAutomationEnabled(true);
        PullRequest known = PullRequest.track(
                story.getId(), repository.getId(), 7, "x", PullRequestState.OPEN, "https://x/7", "PRU-12", now
        );
        when(storyRepository.findPullRequest(story.getId(), repository.getId(), 7)).thenReturn(Optional.of(known));
        page(remote(7, "x", PullRequestState.MERGED, "PRU-12"));
        when(storyRepository.savePullRequest(any())).thenAnswer(call -> call.getArgument(0));

        syncer.sync(repository);

        assertThat(story.getColumnId()).isEqualTo(BoardFixtures.column(board, ColumnKind.DONE).getId());
    }

    @Test
    void successRecordsTheEtagAndTimestampAndClearsTheError() {
        repository.markFailed(now, "RATE_LIMITED");
        page();

        syncer.sync(repository);

        assertThat(repository.getEtag()).isEqualTo("\"etag-1\"");
        assertThat(repository.getLastSyncedAt()).isEqualTo(now);
        assertThat(repository.getLastSyncError()).isNull();
        verify(repositoryPort).save(repository);
    }

    @Test
    void aNotModifiedAnswerKeepsTheEtagAndSkipsEverything() {
        repository.markSynced(now, "\"old\"");
        when(github.listPullRequests("ghp_plain", "arthur", "prumo", "\"old\""))
                .thenReturn(new PullRequestPage(true, "\"old\"", List.of()));

        syncer.sync(repository);

        assertThat(repository.getEtag()).isEqualTo("\"old\"");
        verify(storyRepository, never()).savePullRequest(any());
        verify(boardRepository, never()).findByWorkspaceId(any());
        verify(repositoryPort).save(repository);
    }

    @Test
    void githubErrorsAreRecordedOnTheRepositoryAndDoNotPropagate() {
        when(github.listPullRequests(any(), any(), any(), any()))
                .thenThrow(new GithubIntegrationException(GithubFailure.RATE_LIMITED, "limit"));

        syncer.sync(repository);

        assertThat(repository.getLastSyncError()).isEqualTo("RATE_LIMITED");
        verify(repositoryPort).save(repository);
        verify(storyRepository, never()).savePullRequest(any());
    }

    @Test
    void aMissingTokenIsRecordedWithoutCallingGithub() {
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        syncer.sync(repository);

        assertThat(repository.getLastSyncError()).isEqualTo("MISSING_TOKEN");
        verify(github, never()).listPullRequests(any(), any(), any(), any());
    }

    @Test
    void syncAllKeepsGoingWhenOneRepositoryBlowsUp() {
        ConnectedRepository broken = ConnectedRepository.connect(workspace.getId(), "a", "broken", "main", "u", userId);
        when(repositoryPort.findAll()).thenReturn(List.of(broken, repository));
        when(github.listPullRequests("ghp_plain", "a", "broken", null)).thenThrow(new IllegalStateException("boom"));
        when(github.listPullRequests("ghp_plain", "arthur", "prumo", null))
                .thenReturn(new PullRequestPage(false, "e", List.of()));

        new SyncAllPullRequestsService(repositoryPort, syncer).execute();

        assertThat(broken.getLastSyncError()).isEqualTo("UNEXPECTED");
        assertThat(repository.getEtag()).isEqualTo("e");
    }
}
