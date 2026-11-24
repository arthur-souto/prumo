package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.activity.Activity;

import java.util.List;
import java.util.UUID;

public interface ActivityRepositoryPort {

    Activity save(Activity activity);

    /** Newest first, including the entries of the story's subtasks. */
    List<Activity> findByStoryId(UUID storyId, int limit);

    /** Newest first, only the entries about one subtask. */
    List<Activity> findBySubtaskKey(UUID workspaceId, String subtaskKey, int limit);
}
