package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.auth;

import io.github.arthursouto.prumo.domain.model.user.Role;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String name,
        String email,
        Role role,
        String avatarUrl,
        Instant createdAt
) implements Serializable {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getCreatedAt()
        );
    }
}
