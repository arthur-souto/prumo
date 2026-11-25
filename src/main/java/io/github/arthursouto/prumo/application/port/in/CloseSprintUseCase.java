package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.SprintCommand;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;

public interface CloseSprintUseCase {

    SprintView execute(SprintCommand command);
}
