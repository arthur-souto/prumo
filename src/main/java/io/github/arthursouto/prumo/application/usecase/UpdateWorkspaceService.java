package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.UpdateWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.command.UpdateWorkspaceCommand;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

public class UpdateWorkspaceService implements UpdateWorkspaceUseCase {

    private final WorkspaceRepositoryPort workspaceRepository;

    public UpdateWorkspaceService(WorkspaceRepositoryPort workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public Workspace execute(UpdateWorkspaceCommand command) {
        Workspace workspace = workspaceRepository.findById(command.workspaceId())
                .filter(found -> found.hasMember(command.userId()))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        if (!workspace.canEdit(command.userId())) {
            throw new ForbiddenException("Only owners and admins can edit a workspace");
        }

        workspace.update(
                command.name().strip(),
                command.description() == null || command.description().isBlank()
                        ? null
                        : command.description().strip()
        );

        return workspaceRepository.save(workspace);
    }
}
