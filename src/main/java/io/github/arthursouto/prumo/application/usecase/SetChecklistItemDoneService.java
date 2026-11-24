package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.SetChecklistItemDoneUseCase;
import io.github.arthursouto.prumo.application.port.in.command.SetChecklistItemDoneCommand;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class SetChecklistItemDoneService implements SetChecklistItemDoneUseCase {

    private final StoryLookup stories;
    private final StoryRepositoryPort storyRepository;

    public SetChecklistItemDoneService(StoryLookup stories, StoryRepositoryPort storyRepository) {
        this.stories = stories;
        this.storyRepository = storyRepository;
    }

    @Override
    public UserStory execute(SetChecklistItemDoneCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.key());

        if (!story.setChecklistItemDone(command.itemId(), command.done())) {
            throw new ResourceNotFoundException("Checklist item not found");
        }

        return storyRepository.save(story);
    }
}
