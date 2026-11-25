package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.UpdateSprintCommand;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;

public interface UpdateSprintUseCase {

    SprintView execute(UpdateSprintCommand command);
}
