package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.DeleteStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.DeleteStoryCommand;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;

public class DeleteStoryService implements DeleteStoryUseCase {

    private final StoryLookup stories;
    private final StoryRepositoryPort storyRepository;

    public DeleteStoryService(StoryLookup stories, StoryRepositoryPort storyRepository) {
        this.stories = stories;
        this.storyRepository = storyRepository;
    }

    @Override
    public void execute(DeleteStoryCommand command) {
        storyRepository.deleteById(stories.asMember(command.userId(), command.workspaceId(), command.key()).getId());
    }
}
