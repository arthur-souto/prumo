package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.GetCurrentUserQuery;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    private GetCurrentUserService service;

    @BeforeEach
    void setUp() {
        service = new GetCurrentUserService(userRepository);
    }

    @Test
    void returnsTheUserOfTheQuery() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(service.execute(new GetCurrentUserQuery(user.getId()))).isSameAs(user);
    }

    @Test
    void failsWhenTheUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new GetCurrentUserQuery(userId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }
}
