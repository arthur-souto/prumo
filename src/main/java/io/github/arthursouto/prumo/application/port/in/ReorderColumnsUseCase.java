package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ReorderColumnsCommand;
import io.github.arthursouto.prumo.domain.model.board.Board;

public interface ReorderColumnsUseCase {

    Board execute(ReorderColumnsCommand command);
}
