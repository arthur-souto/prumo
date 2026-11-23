package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record UpdateWorkspaceCommand(UUID userId, UUID workspaceId, String name, String description) {
}
