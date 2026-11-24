package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record DisconnectRepositoryCommand(UUID userId, UUID workspaceId, UUID repositoryId) {
}
