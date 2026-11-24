package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record EditCommentCommand(UUID userId, UUID workspaceId, UUID commentId, String body) {
}
