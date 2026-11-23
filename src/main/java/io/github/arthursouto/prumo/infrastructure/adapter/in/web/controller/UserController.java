package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.GetCurrentUserUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetCurrentUserQuery;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.UserApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserController implements UserApi {

    private final GetCurrentUserUseCase getCurrentUser;

    public UserController(GetCurrentUserUseCase getCurrentUser) {
        this.getCurrentUser = getCurrentUser;
    }

    @Override
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(getCurrentUser.execute(new GetCurrentUserQuery(UUID.fromString(authentication.getName()))));
    }
}
