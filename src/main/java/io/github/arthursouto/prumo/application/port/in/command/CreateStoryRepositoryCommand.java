package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record CreateStoryRepositoryCommand(
        UUID userId,
        UUID workspaceId,
        String key,
        String name,
        String description,
        boolean isPrivate
) {
}
