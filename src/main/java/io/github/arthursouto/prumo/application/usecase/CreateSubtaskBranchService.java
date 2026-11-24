package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.CreateSubtaskBranchUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskBranchCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class CreateSubtaskBranchService implements CreateSubtaskBranchUseCase {

    private final SubtaskLookup lookup;
    private final SubtaskRepositoryPort subtaskRepository;
    private final ConnectedRepositoryPort repositoryPort;
    private final GithubTokens tokens;
    private final GithubPort github;

    public CreateSubtaskBranchService(SubtaskLookup lookup,
                                      SubtaskRepositoryPort subtaskRepository,
                                      ConnectedRepositoryPort repositoryPort,
                                      GithubTokens tokens,
                                      GithubPort github) {
        this.lookup = lookup;
        this.subtaskRepository = subtaskRepository;
        this.repositoryPort = repositoryPort;
        this.tokens = tokens;
        this.github = github;
    }

    @Override
    public SubtaskView execute(CreateSubtaskBranchCommand command) {
        Subtask subtask = lookup.asMember(command.userId(), command.workspaceId(), command.key());
        UserStory story = lookup.storyOf(subtask);

        if (subtask.getBranchName() != null) {
            throw new BusinessRuleException("A subtask can have only one branch");
        }

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
        subtask.recordBranch(branch);

        return lookup.view(subtaskRepository.save(subtask), story);
    }
}
