package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ExchangeOAuthCodeCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;

public interface ExchangeOAuthCodeUseCase {

    AuthTokens execute(ExchangeOAuthCodeCommand command);
}
