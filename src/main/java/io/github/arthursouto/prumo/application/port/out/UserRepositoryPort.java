package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByProvider(AuthProvider authProvider, String providerId);

    boolean existsByEmail(String email);
}
