package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.DeleteWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.command.DeleteWorkspaceCommand;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

public class DeleteWorkspaceService implements DeleteWorkspaceUseCase {

    private final WorkspaceRepositoryPort workspaceRepository;

    public DeleteWorkspaceService(WorkspaceRepositoryPort workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public void execute(DeleteWorkspaceCommand command) {
        Workspace workspace = workspaceRepository.findById(command.workspaceId())
                .filter(found -> found.hasMember(command.userId()))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        if (!workspace.canDelete(command.userId())) {
            throw new ForbiddenException("Only the owner can delete a workspace");
        }

        workspaceRepository.deleteById(workspace.getId());
    }
}
