package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.CreateColumnUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateColumnCommand;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;

public class CreateColumnService implements CreateColumnUseCase {

    private final WorkspaceAccess access;
    private final BoardProvisioner boards;
    private final BoardRepositoryPort boardRepository;

    public CreateColumnService(WorkspaceAccess access, BoardProvisioner boards, BoardRepositoryPort boardRepository) {
        this.access = access;
        this.boards = boards;
        this.boardRepository = boardRepository;
    }

    @Override
    public BoardColumn execute(CreateColumnCommand command) {
        Board board = boards.ensure(access.editor(command.userId(), command.workspaceId()));
        BoardColumn column = board.addColumn(command.name().strip());

        return boardRepository.save(board).require(column.getId());
    }
}
