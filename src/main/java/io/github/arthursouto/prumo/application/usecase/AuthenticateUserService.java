package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.InvalidCredentialsException;
import io.github.arthursouto.prumo.application.port.in.AuthenticateUserUseCase;
import io.github.arthursouto.prumo.application.port.in.command.AuthenticateUserCommand;
import io.github.arthursouto.prumo.application.port.out.PasswordHasherPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.util.Locale;

public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    public AuthenticateUserService(UserRepositoryPort userRepository, PasswordHasherPort passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User execute(AuthenticateUserCommand command) {
        String email = command.email().strip().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

        if (!user.hasPassword() || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
}
