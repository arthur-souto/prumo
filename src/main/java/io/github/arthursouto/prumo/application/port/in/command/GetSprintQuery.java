package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record GetSprintQuery(UUID userId, UUID workspaceId, UUID sprintId) {
}
