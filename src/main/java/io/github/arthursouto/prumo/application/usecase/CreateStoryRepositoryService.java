package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.CreateStoryRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryRepositoryCommand;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class CreateStoryRepositoryService implements CreateStoryRepositoryUseCase {

    private final StoryLookup stories;
    private final StoryRepositoryPort storyRepository;
    private final GithubTokens tokens;
    private final GithubPort github;
    private final RepositoryConnector connector;

    public CreateStoryRepositoryService(StoryLookup stories,
                                        StoryRepositoryPort storyRepository,
                                        GithubTokens tokens,
                                        GithubPort github,
                                        RepositoryConnector connector) {
        this.stories = stories;
        this.storyRepository = storyRepository;
        this.tokens = tokens;
        this.github = github;
        this.connector = connector;
    }

    @Override
    public UserStory execute(CreateStoryRepositoryCommand command) {
        UserStory story = stories.asEditor(command.userId(), command.workspaceId(), command.key());
        RemoteRepository remote = github.createRepository(
                tokens.of(command.userId()),
                command.name().strip(),
                Checklists.text(command.description()),
                command.isPrivate()
        );
        ConnectedRepository repository = connector.connect(command.workspaceId(), command.userId(), remote);

        story.linkRepository(repository.getId());

        return storyRepository.save(story);
    }
}
