package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.AssignStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.AssignStoryCommand;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.activity.ActivityType;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.Objects;
import java.util.UUID;

public class AssignStoryService implements AssignStoryUseCase {

    private final StoryLookup stories;
    private final WorkspaceAccess access;
    private final StoryRepositoryPort storyRepository;
    private final ActivityLog activity;

    public AssignStoryService(StoryLookup stories, WorkspaceAccess access, StoryRepositoryPort storyRepository, ActivityLog activity) {
        this.activity = activity;
        this.stories = stories;
        this.access = access;
        this.storyRepository = storyRepository;
    }

    @Override
    public UserStory execute(AssignStoryCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.key());
        UUID before = story.getAssigneeId();

        if (command.assigneeId() == null) {
            story.assignTo(null);
        } else {
            if (!access.member(command.userId(), command.workspaceId()).hasMember(command.assigneeId())) {
                throw new ResourceNotFoundException("The assignee is not a member of this workspace");
            }
            story.assignTo(command.assigneeId());
        }

        UserStory saved = storyRepository.save(story);

        if (!Objects.equals(before, saved.getAssigneeId())) {
            activity.record(saved.getWorkspaceId(), saved.getId(), Actor.user(command.userId()), ActivityType.STORY_ASSIGNED,
                    before == null ? null : before.toString(), saved.getAssigneeId() == null ? null : saved.getAssigneeId().toString());
        }

        return saved;
    }
}
