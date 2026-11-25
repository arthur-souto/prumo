package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

import java.util.UUID;

public class SprintLookup {

    private final WorkspaceAccess access;
    private final SprintRepositoryPort sprintRepository;

    public SprintLookup(WorkspaceAccess access, SprintRepositoryPort sprintRepository) {
        this.access = access;
        this.sprintRepository = sprintRepository;
    }

    public Sprint asMember(UUID userId, UUID workspaceId, UUID sprintId) {
        access.member(userId, workspaceId);
        return find(workspaceId, sprintId);
    }

    public Sprint asEditor(UUID userId, UUID workspaceId, UUID sprintId) {
        access.editor(userId, workspaceId);
        return find(workspaceId, sprintId);
    }

    public Sprint find(UUID workspaceId, UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .filter(sprint -> sprint.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
    }
}
