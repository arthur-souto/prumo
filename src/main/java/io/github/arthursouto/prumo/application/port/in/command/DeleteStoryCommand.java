package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record DeleteStoryCommand(UUID userId, UUID workspaceId, String key) {
}
