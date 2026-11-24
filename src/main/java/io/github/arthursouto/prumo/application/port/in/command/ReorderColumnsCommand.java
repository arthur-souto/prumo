package io.github.arthursouto.prumo.application.port.in.command;

import java.util.List;
import java.util.UUID;

public record ReorderColumnsCommand(UUID userId, UUID workspaceId, List<UUID> columnIds) {
}
