package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.UpdateBoardSettingsCommand;
import io.github.arthursouto.prumo.domain.model.board.Board;

public interface UpdateBoardSettingsUseCase {

    Board execute(UpdateBoardSettingsCommand command);
}
