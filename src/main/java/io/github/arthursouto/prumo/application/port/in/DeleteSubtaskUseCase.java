package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.DeleteSubtaskCommand;

public interface DeleteSubtaskUseCase {

    void execute(DeleteSubtaskCommand command);
}
