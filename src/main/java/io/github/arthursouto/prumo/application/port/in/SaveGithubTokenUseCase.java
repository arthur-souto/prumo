package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.SaveGithubTokenCommand;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;

public interface SaveGithubTokenUseCase {

    GithubConnection execute(SaveGithubTokenCommand command);
}
