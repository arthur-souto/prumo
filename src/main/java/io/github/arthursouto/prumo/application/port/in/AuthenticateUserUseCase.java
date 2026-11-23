package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.AuthenticateUserCommand;
import io.github.arthursouto.prumo.domain.model.user.User;

public interface AuthenticateUserUseCase {

    User execute(AuthenticateUserCommand command);
}
