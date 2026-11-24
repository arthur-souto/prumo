package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.AssignStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.AssignStoryCommand;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class AssignStoryService implements AssignStoryUseCase {

    private final StoryLookup stories;
    private final WorkspaceAccess access;
    private final StoryRepositoryPort storyRepository;

    public AssignStoryService(StoryLookup stories, WorkspaceAccess access, StoryRepositoryPort storyRepository) {
        this.stories = stories;
        this.access = access;
        this.storyRepository = storyRepository;
    }

    @Override
    public UserStory execute(AssignStoryCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.key());

        if (command.assigneeId() == null) {
            story.assignTo(null);
        } else {
            if (!access.member(command.userId(), command.workspaceId()).hasMember(command.assigneeId())) {
                throw new ResourceNotFoundException("The assignee is not a member of this workspace");
            }
            story.assignTo(command.assigneeId());
        }

        return storyRepository.save(story);
    }
}
