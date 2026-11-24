package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.KeyPrefixTakenException;
import io.github.arthursouto.prumo.application.port.in.UpdateBoardSettingsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.UpdateBoardSettingsCommand;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

import java.util.Locale;

public class UpdateBoardSettingsService implements UpdateBoardSettingsUseCase {

    private final WorkspaceAccess access;
    private final BoardProvisioner boards;
    private final BoardRepositoryPort boardRepository;
    private final StoryRepositoryPort storyRepository;

    public UpdateBoardSettingsService(WorkspaceAccess access,
                                      BoardProvisioner boards,
                                      BoardRepositoryPort boardRepository,
                                      StoryRepositoryPort storyRepository) {
        this.access = access;
        this.boards = boards;
        this.boardRepository = boardRepository;
        this.storyRepository = storyRepository;
    }

    @Override
    public Board execute(UpdateBoardSettingsCommand command) {
        Workspace workspace = access.editor(command.userId(), command.workspaceId());
        Board board = boards.ensure(workspace);

        if (command.keyPrefix() != null) {
            changePrefix(board, command.keyPrefix().strip().toUpperCase(Locale.ROOT));
        }

        if (command.automationEnabled() != null) {
            board.setAutomationEnabled(command.automationEnabled());
        }

        try {
            return boardRepository.save(board);
        } catch (KeyPrefixTakenException exception) {
            throw new ConflictException("This key prefix is already used by another workspace");
        }
    }

    private void changePrefix(Board board, String prefix) {
        if (prefix.equals(board.getKeyPrefix())) {
            return;
        }

        if (storyRepository.countByWorkspaceId(board.getWorkspaceId()) > 0) {
            throw new BusinessRuleException("The key prefix cannot change once the workspace has stories");
        }

        if (boardRepository.existsByKeyPrefix(prefix)) {
            throw new ConflictException("This key prefix is already used by another workspace");
        }

        board.changeKeyPrefix(prefix);
    }
}
