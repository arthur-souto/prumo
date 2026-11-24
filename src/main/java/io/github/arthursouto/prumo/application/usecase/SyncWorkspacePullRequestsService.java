package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.SyncWorkspacePullRequestsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.SyncWorkspacePullRequestsCommand;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;

import java.util.List;

public class SyncWorkspacePullRequestsService implements SyncWorkspacePullRequestsUseCase {

    private final WorkspaceAccess access;
    private final ConnectedRepositoryPort repositoryPort;
    private final PullRequestSyncer syncer;

    public SyncWorkspacePullRequestsService(WorkspaceAccess access,
                                            ConnectedRepositoryPort repositoryPort,
                                            PullRequestSyncer syncer) {
        this.access = access;
        this.repositoryPort = repositoryPort;
        this.syncer = syncer;
    }

    @Override
    public List<ConnectedRepository> execute(SyncWorkspacePullRequestsCommand command) {
        access.member(command.userId(), command.workspaceId());
        repositoryPort.findAllByWorkspaceId(command.workspaceId()).forEach(syncer::sync);

        return repositoryPort.findAllByWorkspaceId(command.workspaceId());
    }
}
