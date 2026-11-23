package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.InvalidCredentialsException;
import io.github.arthursouto.prumo.application.port.in.command.AuthenticateUserCommand;
import io.github.arthursouto.prumo.application.port.out.PasswordHasherPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordHasherPort passwordHasher;

    private AuthenticateUserService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticateUserService(userRepository, passwordHasher);
    }

    @Test
    void returnsUserWhenCredentialsAreValid() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        when(userRepository.findByEmail("arthur@prumo.io")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("s3cret-pass", "hashed")).thenReturn(true);

        User authenticated = service.execute(new AuthenticateUserCommand("Arthur@Prumo.io", "s3cret-pass"));

        assertThat(authenticated).isSameAs(user);
    }

    @Test
    void rejectsUnknownEmail() {
        when(userRepository.findByEmail("ghost@prumo.io")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new AuthenticateUserCommand("ghost@prumo.io", "s3cret-pass")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void rejectsWrongPassword() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        when(userRepository.findByEmail("arthur@prumo.io")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-pass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.execute(new AuthenticateUserCommand("arthur@prumo.io", "wrong-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsUserWithoutPassword() {
        User user = User.registerExternal("Arthur", "arthur@prumo.io", null);
        when(userRepository.findByEmail("arthur@prumo.io")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.execute(new AuthenticateUserCommand("arthur@prumo.io", "s3cret-pass")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordHasher, never()).matches(any(), any());
    }

    @Test
    void rejectsDisabledUser() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        user.disable();
        when(userRepository.findByEmail("arthur@prumo.io")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.execute(new AuthenticateUserCommand("arthur@prumo.io", "s3cret-pass")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordHasher, never()).matches(any(), any());
    }
}
