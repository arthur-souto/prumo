package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.RegisterUserCommand;
import io.github.arthursouto.prumo.application.port.out.PasswordHasherPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.model.user.Role;
import io.github.arthursouto.prumo.domain.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordHasherPort passwordHasher;

    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(userRepository, passwordHasher);
    }

    @Test
    void registersLocalUserWithHashedPasswordAndLowerCaseEmail() {
        when(userRepository.existsByEmail("arthur@prumo.io")).thenReturn(false);
        when(passwordHasher.hash("s3cret-pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.execute(new RegisterUserCommand("  Arthur  ", "Arthur@Prumo.io", "s3cret-pass"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(user);
        assertThat(user.getName()).isEqualTo("Arthur");
        assertThat(user.getEmail()).isEqualTo("arthur@prumo.io");
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void rejectsEmailThatIsAlreadyRegistered() {
        when(userRepository.existsByEmail("arthur@prumo.io")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(new RegisterUserCommand("Arthur", "arthur@prumo.io", "s3cret-pass")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordHasher, never()).hash(any());
    }
}
