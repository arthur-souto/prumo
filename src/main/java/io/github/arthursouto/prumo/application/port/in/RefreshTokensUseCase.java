package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.RefreshTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;

public interface RefreshTokensUseCase {

    AuthTokens execute(RefreshTokensCommand command);
}
