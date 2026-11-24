package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record RenameColumnCommand(UUID userId, UUID workspaceId, UUID columnId, String name) {
}
