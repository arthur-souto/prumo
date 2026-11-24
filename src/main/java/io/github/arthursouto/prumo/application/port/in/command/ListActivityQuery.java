package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

/** The history of a story (with its subtasks) or, with a subtask key, only of that subtask. */
public record ListActivityQuery(UUID userId, UUID workspaceId, String storyKey, String subtaskKey) {

    public static ListActivityQuery ofStory(UUID userId, UUID workspaceId, String storyKey) {
        return new ListActivityQuery(userId, workspaceId, storyKey, null);
    }

    public static ListActivityQuery ofSubtask(UUID userId, UUID workspaceId, String subtaskKey) {
        return new ListActivityQuery(userId, workspaceId, null, subtaskKey);
    }
}
