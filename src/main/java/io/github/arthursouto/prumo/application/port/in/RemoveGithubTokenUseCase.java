package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.RemoveGithubTokenCommand;

public interface RemoveGithubTokenUseCase {

    void execute(RemoveGithubTokenCommand command);
}
