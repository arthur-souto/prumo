package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListConnectedRepositoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListConnectedRepositoriesQuery;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;

import java.util.List;

public class ListConnectedRepositoriesService implements ListConnectedRepositoriesUseCase {

    private final WorkspaceAccess access;
    private final ConnectedRepositoryPort repositoryPort;

    public ListConnectedRepositoriesService(WorkspaceAccess access, ConnectedRepositoryPort repositoryPort) {
        this.access = access;
        this.repositoryPort = repositoryPort;
    }

    @Override
    public List<ConnectedRepository> execute(ListConnectedRepositoriesQuery query) {
        access.member(query.userId(), query.workspaceId());

        return repositoryPort.findAllByWorkspaceId(query.workspaceId());
    }
}
