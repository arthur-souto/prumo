package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ConnectRepositoryCommand;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;

public interface ConnectRepositoryUseCase {

    ConnectedRepository execute(ConnectRepositoryCommand command);
}
