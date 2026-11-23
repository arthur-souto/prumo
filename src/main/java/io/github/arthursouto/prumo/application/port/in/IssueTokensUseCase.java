package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.IssueTokensCommand;
import io.github.arthursouto.prumo.application.port.in.result.AuthTokens;

public interface IssueTokensUseCase {

    AuthTokens execute(IssueTokensCommand command);
}
