package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.DisconnectRepositoryCommand;

public interface DisconnectRepositoryUseCase {

    void execute(DisconnectRepositoryCommand command);
}
