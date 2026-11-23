package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.RevokeRefreshTokenCommand;

public interface RevokeRefreshTokenUseCase {

    void execute(RevokeRefreshTokenCommand command);
}
