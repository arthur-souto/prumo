package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.auth.AccessToken;
import io.github.arthursouto.prumo.domain.model.user.User;

public interface AccessTokenIssuerPort {

    AccessToken issue(User user);
}
