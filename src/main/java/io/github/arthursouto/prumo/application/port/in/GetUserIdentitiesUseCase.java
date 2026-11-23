package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetUserIdentitiesQuery;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;

import java.util.List;

public interface GetUserIdentitiesUseCase {

    List<UserIdentity> execute(GetUserIdentitiesQuery query);
}
