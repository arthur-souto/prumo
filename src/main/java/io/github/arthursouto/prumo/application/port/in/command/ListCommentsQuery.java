package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

/** The comments of a story or, when subtaskKey is set, of that subtask. */
public record ListCommentsQuery(UUID userId, UUID workspaceId, String storyKey, String subtaskKey) {
}
