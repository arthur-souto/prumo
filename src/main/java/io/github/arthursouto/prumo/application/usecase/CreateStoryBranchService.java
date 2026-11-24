package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.CreateStoryBranchUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryBranchCommand;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class CreateStoryBranchService implements CreateStoryBranchUseCase {

    private final StoryLookup stories;
    private final StoryRepositoryPort storyRepository;
    private final ConnectedRepositoryPort repositoryPort;
    private final GithubTokens tokens;
    private final GithubPort github;

    public CreateStoryBranchService(StoryLookup stories,
                                    StoryRepositoryPort storyRepository,
                                    ConnectedRepositoryPort repositoryPort,
                                    GithubTokens tokens,
                                    GithubPort github) {
        this.stories = stories;
        this.storyRepository = storyRepository;
        this.repositoryPort = repositoryPort;
        this.tokens = tokens;
        this.github = github;
    }

    @Override
    public UserStory execute(CreateStoryBranchCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.key());

        if (story.getRepositoryId() == null) {
            throw new BusinessRuleException("Link a repository to the story before creating a branch");
        }

        ConnectedRepository repository = repositoryPort.findById(story.getRepositoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));
        String branch = command.branchName().strip();

        github.createBranch(
                tokens.of(command.userId()),
                repository.getOwner(),
                repository.getName(),
                branch,
                repository.getDefaultBranch()
        );
        story.recordBranch(branch);

        return storyRepository.save(story);
    }
}
