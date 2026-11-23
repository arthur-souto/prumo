package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListWorkspacesUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListWorkspacesQuery;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

import java.util.List;

public class ListWorkspacesService implements ListWorkspacesUseCase {

    private final WorkspaceRepositoryPort workspaceRepository;

    public ListWorkspacesService(WorkspaceRepositoryPort workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public List<Workspace> execute(ListWorkspacesQuery query) {
        return workspaceRepository.findAllByMemberUserId(query.userId());
    }
}
