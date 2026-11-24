package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.RenameColumnUseCase;
import io.github.arthursouto.prumo.application.port.in.command.RenameColumnCommand;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;

public class RenameColumnService implements RenameColumnUseCase {

    private final WorkspaceAccess access;
    private final BoardProvisioner boards;
    private final BoardRepositoryPort boardRepository;

    public RenameColumnService(WorkspaceAccess access, BoardProvisioner boards, BoardRepositoryPort boardRepository) {
        this.access = access;
        this.boards = boards;
        this.boardRepository = boardRepository;
    }

    @Override
    public BoardColumn execute(RenameColumnCommand command) {
        Board board = boards.ensure(access.editor(command.userId(), command.workspaceId()));
        board.rename(command.columnId(), command.name().strip());

        return boardRepository.save(board).require(command.columnId());
    }
}
