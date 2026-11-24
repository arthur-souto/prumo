package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.DisconnectRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.DisconnectRepositoryCommand;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;

public class DisconnectRepositoryService implements DisconnectRepositoryUseCase {

    private final WorkspaceAccess access;
    private final ConnectedRepositoryPort repositoryPort;

    public DisconnectRepositoryService(WorkspaceAccess access, ConnectedRepositoryPort repositoryPort) {
        this.access = access;
        this.repositoryPort = repositoryPort;
    }

    @Override
    public void execute(DisconnectRepositoryCommand command) {
        access.editor(command.userId(), command.workspaceId());

        repositoryPort.findById(command.repositoryId())
                .filter(repository -> repository.getWorkspaceId().equals(command.workspaceId()))
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));

        repositoryPort.deleteById(command.repositoryId());
    }
}
