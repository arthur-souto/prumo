package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record DeleteWorkspaceCommand(UUID userId, UUID workspaceId) {
}
