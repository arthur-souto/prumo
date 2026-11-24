package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.DeleteColumnCommand;

public interface DeleteColumnUseCase {

    void execute(DeleteColumnCommand command);
}
