package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record DeleteColumnCommand(UUID userId, UUID workspaceId, UUID columnId, UUID moveToColumnId) {
}
