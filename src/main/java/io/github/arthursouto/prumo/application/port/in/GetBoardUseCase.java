package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetBoardQuery;
import io.github.arthursouto.prumo.application.port.in.result.BoardView;

public interface GetBoardUseCase {

    BoardView execute(GetBoardQuery command);
}
