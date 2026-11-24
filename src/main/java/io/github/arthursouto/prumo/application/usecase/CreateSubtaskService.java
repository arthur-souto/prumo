package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.CreateSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

public class CreateSubtaskService implements CreateSubtaskUseCase {

    private final StoryLookup stories;
    private final SubtaskLookup lookup;
    private final BoardProvisioner boards;
    private final BoardRepositoryPort boardRepository;
    private final SubtaskRepositoryPort subtaskRepository;
    private final ConnectedRepositoryPort repositoryPort;
    private final GithubTokens tokens;
    private final GithubPort github;

    public CreateSubtaskService(StoryLookup stories,
                                SubtaskLookup lookup,
                                BoardProvisioner boards,
                                BoardRepositoryPort boardRepository,
                                SubtaskRepositoryPort subtaskRepository,
                                ConnectedRepositoryPort repositoryPort,
                                GithubTokens tokens,
                                GithubPort github) {
        this.stories = stories;
        this.lookup = lookup;
        this.boards = boards;
        this.boardRepository = boardRepository;
        this.subtaskRepository = subtaskRepository;
        this.repositoryPort = repositoryPort;
        this.tokens = tokens;
        this.github = github;
    }

    @Override
    public SubtaskView execute(CreateSubtaskCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.storyKey());
        ConnectedRepository repository = null;
        String branch = null;

        if (command.createBranch()) {
            if (story.getRepositoryId() == null) {
                throw new BusinessRuleException("Link a repository to the story before creating a branch");
            }

            repository = repositoryPort.findById(story.getRepositoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));
            branch = Checklists.text(command.branchName());

            if (branch == null) {
                throw new BusinessRuleException("Give the branch a name");
            }
        }

        Workspace workspace = lookup.workspace(command.userId(), command.workspaceId());
        Board board = boards.ensure(workspace);
        int number = boardRepository.allocateStoryNumber(workspace.getId());
        int position = subtaskRepository.findAllByStoryId(story.getId()).size();
        Subtask subtask = Subtask.create(
                story.getId(),
                workspace.getId(),
                number,
                board.getKeyPrefix() + "-" + number,
                command.title().strip(),
                Checklists.text(command.description()),
                position
        );

        if (repository != null) {
            // GitHub first: if it fails, no subtask is left behind without the branch the user asked for
            github.createBranch(
                    tokens.of(command.userId()),
                    repository.getOwner(),
                    repository.getName(),
                    branch,
                    repository.getDefaultBranch()
            );
            subtask.recordBranch(branch);
        }

        return lookup.view(subtaskRepository.save(subtask), story);
    }
}
