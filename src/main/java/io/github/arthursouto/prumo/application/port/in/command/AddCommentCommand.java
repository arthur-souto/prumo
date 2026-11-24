package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

/** A comment on a story or, when subtaskKey is set, on that subtask. */
public record AddCommentCommand(UUID userId, UUID workspaceId, String storyKey, String subtaskKey, String body) {
}
