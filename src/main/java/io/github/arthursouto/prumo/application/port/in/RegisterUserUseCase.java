package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.RegisterUserCommand;
import io.github.arthursouto.prumo.domain.model.user.User;

public interface RegisterUserUseCase {

    User execute(RegisterUserCommand command);
}
