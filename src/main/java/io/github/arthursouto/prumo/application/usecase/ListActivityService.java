package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListActivityUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListActivityQuery;
import io.github.arthursouto.prumo.application.port.in.result.ActivityView;
import io.github.arthursouto.prumo.application.port.out.ActivityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.activity.Activity;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ListActivityService implements ListActivityUseCase {

    static final int LIMIT = 100;

    private final StoryLookup stories;
    private final SubtaskLookup subtasks;
    private final ActivityRepositoryPort activityRepository;
    private final UserRepositoryPort userRepository;

    public ListActivityService(StoryLookup stories,
                               SubtaskLookup subtasks,
                               ActivityRepositoryPort activityRepository,
                               UserRepositoryPort userRepository) {
        this.stories = stories;
        this.subtasks = subtasks;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ActivityView> execute(ListActivityQuery query) {
        List<Activity> entries;

        if (query.subtaskKey() != null) {
            Subtask subtask = subtasks.asMember(query.userId(), query.workspaceId(), query.subtaskKey());
            entries = activityRepository.findBySubtaskKey(query.workspaceId(), subtask.getKey(), LIMIT);
        } else {
            UserStory story = stories.asMember(query.userId(), query.workspaceId(), query.storyKey());
            entries = activityRepository.findByStoryId(story.getId(), LIMIT);
        }

        Map<UUID, String> names = new HashMap<>();

        return entries.stream()
                .map(entry -> new ActivityView(
                        entry,
                        name(names, entry.actorId()),
                        entry.type() == ActivityType.STORY_ASSIGNED ? name(names, id(entry.fromValue())) : entry.fromValue(),
                        entry.type() == ActivityType.STORY_ASSIGNED ? name(names, id(entry.toValue())) : entry.toValue()))
                .toList();
    }

    private String name(Map<UUID, String> cache, UUID userId) {
        if (userId == null) {
            return null;
        }

        return cache.computeIfAbsent(userId, id -> userRepository.findById(id).map(user -> user.getName()).orElse(null));
    }

    private static UUID id(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
