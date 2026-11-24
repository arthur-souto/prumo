package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ReorderColumnsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ReorderColumnsCommand;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.domain.model.board.Board;

public class ReorderColumnsService implements ReorderColumnsUseCase {

    private final WorkspaceAccess access;
    private final BoardProvisioner boards;
    private final BoardRepositoryPort boardRepository;

    public ReorderColumnsService(WorkspaceAccess access, BoardProvisioner boards, BoardRepositoryPort boardRepository) {
        this.access = access;
        this.boards = boards;
        this.boardRepository = boardRepository;
    }

    @Override
    public Board execute(ReorderColumnsCommand command) {
        Board board = boards.ensure(access.editor(command.userId(), command.workspaceId()));
        board.reorder(command.columnIds());

        return boardRepository.save(board);
    }
}
