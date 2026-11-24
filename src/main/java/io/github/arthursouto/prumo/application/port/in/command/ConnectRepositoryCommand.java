package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record ConnectRepositoryCommand(UUID userId, UUID workspaceId, String owner, String name) {
}
