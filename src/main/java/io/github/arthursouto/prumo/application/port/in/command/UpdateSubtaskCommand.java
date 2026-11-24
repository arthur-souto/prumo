package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record UpdateSubtaskCommand(UUID userId, UUID workspaceId, String key, String title, String description) {
}
