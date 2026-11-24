package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record AssignStoryCommand(UUID userId, UUID workspaceId, String key, UUID assigneeId) {
}
