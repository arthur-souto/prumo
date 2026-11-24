package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.RenameColumnCommand;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;

public interface RenameColumnUseCase {

    BoardColumn execute(RenameColumnCommand command);
}
