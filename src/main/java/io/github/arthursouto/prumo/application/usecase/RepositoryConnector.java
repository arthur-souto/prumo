package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;

import java.util.UUID;

public class RepositoryConnector {

    private final ConnectedRepositoryPort repositoryPort;

    public RepositoryConnector(ConnectedRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public ConnectedRepository connect(UUID workspaceId, UUID userId, RemoteRepository remote) {
        repositoryPort.findByWorkspaceIdAndOwnerAndName(workspaceId, remote.owner(), remote.name())
                .ifPresent(existing -> {
                    throw new ConflictException("This repository is already connected to the workspace");
                });

        return repositoryPort.save(ConnectedRepository.connect(
                workspaceId,
                remote.owner(),
                remote.name(),
                remote.defaultBranch(),
                remote.htmlUrl(),
                userId
        ));
    }
}
