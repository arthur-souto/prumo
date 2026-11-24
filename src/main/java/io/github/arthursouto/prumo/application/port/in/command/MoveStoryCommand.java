package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record MoveStoryCommand(
        UUID userId,
        UUID workspaceId,
        String key,
        UUID columnId,
        UUID afterStoryId,
        UUID beforeStoryId
) {
}
