package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

import java.util.UUID;

public class WorkspaceAccess {

    private final WorkspaceRepositoryPort workspaceRepository;

    public WorkspaceAccess(WorkspaceRepositoryPort workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public Workspace member(UUID userId, UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .filter(workspace -> workspace.hasMember(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    public Workspace editor(UUID userId, UUID workspaceId) {
        Workspace workspace = member(userId, workspaceId);

        if (!workspace.canEdit(userId)) {
            throw new ForbiddenException("Only owners and admins can do this");
        }

        return workspace;
    }
}
