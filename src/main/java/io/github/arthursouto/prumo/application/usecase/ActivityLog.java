package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.ActivityRepositoryPort;
import io.github.arthursouto.prumo.domain.model.activity.Activity;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;

import java.util.UUID;

/** Writes the history entries the use cases and the pull request sync leave behind. */
public class ActivityLog {

    private final ActivityRepositoryPort repository;

    public ActivityLog(ActivityRepositoryPort repository) {
        this.repository = repository;
    }

    public void record(UUID workspaceId, UUID storyId, String subtaskKey, Actor actor, ActivityType type, String from, String to) {
        repository.save(Activity.of(workspaceId, storyId, subtaskKey, actor.userId(), actor.source(), type, from, to));
    }

    public void record(UUID workspaceId, UUID storyId, Actor actor, ActivityType type, String from, String to) {
        record(workspaceId, storyId, null, actor, type, from, to);
    }
}
