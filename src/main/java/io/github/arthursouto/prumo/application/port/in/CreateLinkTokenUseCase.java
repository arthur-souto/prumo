package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.CreateLinkTokenCommand;

public interface CreateLinkTokenUseCase {

    String execute(CreateLinkTokenCommand command);
}
