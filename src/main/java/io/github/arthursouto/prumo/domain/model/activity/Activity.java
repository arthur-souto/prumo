package io.github.arthursouto.prumo.domain.model.activity;

import io.github.arthursouto.prumo.domain.model.story.ChangeSource;

import java.time.Instant;
import java.util.UUID;

/**
 * One entry of a story's history. The actor is null when GitHub made the change. For a move, from and to are the
 * column names; for a status change, the statuses; for an assignment, the user ids; for an update, the changed fields.
 */
public record Activity(
        UUID id,
        UUID workspaceId,
        UUID storyId,
        String subtaskKey,
        UUID actorId,
        ChangeSource source,
        ActivityType type,
        String fromValue,
        String toValue,
        Instant createdAt
) {

    public static Activity of(UUID workspaceId,
                              UUID storyId,
                              String subtaskKey,
                              UUID actorId,
                              ChangeSource source,
                              ActivityType type,
                              String fromValue,
                              String toValue) {
        return new Activity(UUID.randomUUID(), workspaceId, storyId, subtaskKey, actorId, source, type, fromValue, toValue, Instant.now());
    }
}
