package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.CreateColumnCommand;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;

public interface CreateColumnUseCase {

    BoardColumn execute(CreateColumnCommand command);
}
