package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.LinkOAuthIdentityCommand;

public interface LinkOAuthIdentityUseCase {

    void execute(LinkOAuthIdentityCommand command);
}
