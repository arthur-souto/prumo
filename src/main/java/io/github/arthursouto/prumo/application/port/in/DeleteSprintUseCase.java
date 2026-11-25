package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.SprintCommand;

public interface DeleteSprintUseCase {

    void execute(SprintCommand command);
}
