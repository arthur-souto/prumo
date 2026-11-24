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
import io.github.arthursouto.prumo.domain.model.board.Rank;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import io.github.arthursouto.prumo.domain.model.story.StoryKeys;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PullRequestSyncer {

    private final GithubConnectionRepositoryPort connectionRepository;
    private final TokenCipherPort cipher;
    private final GithubPort github;
    private final StoryRepositoryPort storyRepository;
    private final BoardRepositoryPort boardRepository;
    private final ConnectedRepositoryPort repositoryPort;
    private final Clock clock;

    public PullRequestSyncer(GithubConnectionRepositoryPort connectionRepository,
                             TokenCipherPort cipher,
                             GithubPort github,
                             StoryRepositoryPort storyRepository,
                             BoardRepositoryPort boardRepository,
                             ConnectedRepositoryPort repositoryPort,
                             Clock clock) {
        this.connectionRepository = connectionRepository;
        this.cipher = cipher;
        this.github = github;
        this.storyRepository = storyRepository;
        this.boardRepository = boardRepository;
        this.repositoryPort = repositoryPort;
        this.clock = clock;
    }

    public void sync(ConnectedRepository repository) {
        Optional<GithubConnection> connection = connectionRepository.findByUserId(repository.getConnectedBy());

        if (connection.isEmpty()) {
            fail(repository, GithubFailure.MISSING_TOKEN.name());
            return;
        }

        PullRequestPage page;

        try {
            page = github.listPullRequests(
                    cipher.decrypt(connection.get().tokenCipher()),
                    repository.getOwner(),
                    repository.getName(),
                    repository.getEtag()
            );
        } catch (GithubIntegrationException exception) {
            fail(repository, exception.getReason().name());
            return;
        }

        if (!page.notModified()) {
            Board board = boardRepository.findByWorkspaceId(repository.getWorkspaceId()).orElse(null);

            page.items().forEach(remote -> apply(repository, board, remote));
        }

        repository.markSynced(clock.instant(), page.notModified() ? repository.getEtag() : page.etag());
        repositoryPort.save(repository);
    }

    private void fail(ConnectedRepository repository, String code) {
        repository.markFailed(clock.instant(), code);
        repositoryPort.save(repository);
    }

    private void apply(ConnectedRepository repository, Board board, RemotePullRequest remote) {
        Set<String> keys = StoryKeys.extract(remote.branch(), remote.title());

        if (keys.isEmpty()) {
            return;
        }

        for (UserStory story : storyRepository.findAllByWorkspaceIdAndKeys(repository.getWorkspaceId(), keys)) {
            Optional<PullRequest> existing = storyRepository.findPullRequest(
                    story.getId(),
                    repository.getId(),
                    remote.number()
            );
            boolean changed = existing.map(pullRequest -> pullRequest.state() != remote.state()).orElse(true);

            storyRepository.savePullRequest(existing
                    .map(pullRequest -> pullRequest.refreshed(
                            story.getId(),
                            remote.title(),
                            remote.state(),
                            remote.url(),
                            remote.branch(),
                            remote.updatedAt()
                    ))
                    .orElseGet(() -> PullRequest.track(
                            story.getId(),
                            repository.getId(),
                            remote.number(),
                            remote.title(),
                            remote.state(),
                            remote.url(),
                            remote.branch(),
                            remote.updatedAt()
                    )));

            if (changed && board != null && board.isAutomationEnabled()) {
                automate(board, story, remote.state());
            }
        }
    }

    private void automate(Board board, UserStory story, PullRequestState state) {
        ColumnKind target = switch (state) {
            case OPEN -> ColumnKind.IN_REVIEW;
            case MERGED -> ColumnKind.DONE;
            case CLOSED -> null;
        };

        if (target == null) {
            return;
        }

        board.ofKind(target)
                .filter(column -> !column.getId().equals(story.getColumnId()))
                .ifPresent(column -> {
                    List<UserStory> inColumn = storyRepository.findAllByColumnId(column.getId());
                    UUID columnId = column.getId();
                    String last = inColumn.isEmpty() ? null : inColumn.get(inColumn.size() - 1).getRank();

                    story.moveTo(columnId, Rank.between(last, null));
                    storyRepository.save(story);
                });
    }
}
