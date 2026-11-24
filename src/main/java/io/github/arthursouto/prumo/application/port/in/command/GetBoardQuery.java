package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record GetBoardQuery(UUID userId, UUID workspaceId) {
}
