package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetWorkspaceQuery;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

public class GetWorkspaceService implements GetWorkspaceUseCase {

    private final WorkspaceRepositoryPort workspaceRepository;

    public GetWorkspaceService(WorkspaceRepositoryPort workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public Workspace execute(GetWorkspaceQuery query) {
        return workspaceRepository.findById(query.workspaceId())
                .filter(workspace -> workspace.hasMember(query.userId()))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }
}
