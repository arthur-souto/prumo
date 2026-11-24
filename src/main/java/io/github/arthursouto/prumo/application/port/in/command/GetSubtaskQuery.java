package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record GetSubtaskQuery(UUID userId, UUID workspaceId, String key) {
}
