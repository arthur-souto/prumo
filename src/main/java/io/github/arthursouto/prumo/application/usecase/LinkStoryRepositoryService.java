package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.LinkStoryRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.LinkStoryRepositoryCommand;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class LinkStoryRepositoryService implements LinkStoryRepositoryUseCase {

    private final StoryLookup stories;
    private final StoryRepositoryPort storyRepository;
    private final ConnectedRepositoryPort repositoryPort;

    public LinkStoryRepositoryService(StoryLookup stories,
                                      StoryRepositoryPort storyRepository,
                                      ConnectedRepositoryPort repositoryPort) {
        this.stories = stories;
        this.storyRepository = storyRepository;
        this.repositoryPort = repositoryPort;
    }

    @Override
    public UserStory execute(LinkStoryRepositoryCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.key());

        if (command.repositoryId() == null) {
            story.unlinkRepository();
        } else {
            repositoryPort.findById(command.repositoryId())
                    .filter(repository -> repository.getWorkspaceId().equals(command.workspaceId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));
            story.linkRepository(command.repositoryId());
        }

        return storyRepository.save(story);
    }
}
