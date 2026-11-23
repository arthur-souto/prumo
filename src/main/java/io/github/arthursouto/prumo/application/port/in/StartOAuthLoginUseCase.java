package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.StartOAuthLoginCommand;

public interface StartOAuthLoginUseCase {

    String execute(StartOAuthLoginCommand command);
}
