package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record SetChecklistItemDoneCommand(UUID userId, UUID workspaceId, String key, UUID itemId, boolean done) {
}
