package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetCurrentUserQuery;
import io.github.arthursouto.prumo.domain.model.user.User;

public interface GetCurrentUserUseCase {

    User execute(GetCurrentUserQuery query);
}
