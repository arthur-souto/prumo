package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.CreateSprintCommand;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;

public interface CreateSprintUseCase {

    SprintView execute(CreateSprintCommand command);
}
