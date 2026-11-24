package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.UpdateStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.UpdateStoryCommand;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UpdateStoryService implements UpdateStoryUseCase {

    private final StoryLookup stories;
    private final StoryRepositoryPort storyRepository;
    private final ActivityLog activity;

    public UpdateStoryService(StoryLookup stories, StoryRepositoryPort storyRepository, ActivityLog activity) {
        this.stories = stories;
        this.storyRepository = storyRepository;
        this.activity = activity;
    }

    @Override
    public UserStory execute(UpdateStoryCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.key());
        Snapshot before = Snapshot.of(story);

        story.update(
                command.title().strip(),
                Checklists.text(command.description()),
                command.priority() == null ? StoryPriority.MEDIUM : command.priority(),
                command.storyPoints(),
                Checklists.labels(command.labels()),
                Checklists.items(command.acceptanceCriteria(), story.getAcceptanceCriteria())
        );

        UserStory saved = storyRepository.save(story);
        List<String> changed = before.changedFields(saved);

        if (!changed.isEmpty()) {
            activity.record(saved.getWorkspaceId(), saved.getId(), Actor.user(command.userId()), ActivityType.STORY_UPDATED,
                    null, String.join(",", changed));
        }

        if (!Objects.equals(before.points(), saved.getStoryPoints())) {
            activity.record(saved.getWorkspaceId(), saved.getId(), Actor.user(command.userId()), ActivityType.STORY_POINTS_CHANGED,
                    pointsText(before.points()), pointsText(saved.getStoryPoints()));
        }

        return saved;
    }

    /** The points as shown in the history; null means the story has no estimate. */
    private static String pointsText(Integer points) {
        return points == null ? null : String.valueOf(points);
    }

    /** The editable fields before the update, to tell which ones it changed. */
    private record Snapshot(String title, String description, StoryPriority priority, Integer points, Object labels, Object criteria) {

        static Snapshot of(UserStory story) {
            return new Snapshot(story.getTitle(), story.getDescription(), story.getPriority(), story.getStoryPoints(),
                    story.getLabels(), story.getAcceptanceCriteria());
        }

        List<String> changedFields(UserStory after) {
            List<String> changed = new ArrayList<>();

            if (!Objects.equals(title, after.getTitle())) changed.add("title");
            if (!Objects.equals(description, after.getDescription())) changed.add("description");
            if (!Objects.equals(priority, after.getPriority())) changed.add("priority");
            if (!Objects.equals(labels, after.getLabels())) changed.add("labels");
            if (!Objects.equals(criteria, after.getAcceptanceCriteria())) changed.add("criteria");

            return changed;
        }
    }
}
