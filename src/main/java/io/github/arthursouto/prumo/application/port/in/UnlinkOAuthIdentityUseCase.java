package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.UnlinkOAuthIdentityCommand;

public interface UnlinkOAuthIdentityUseCase {

    void execute(UnlinkOAuthIdentityCommand command);
}
