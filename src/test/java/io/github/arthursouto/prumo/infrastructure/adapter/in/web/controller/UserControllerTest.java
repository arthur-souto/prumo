package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.GetCurrentUserUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetCurrentUserQuery;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private GetCurrentUserUseCase getCurrentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(getCurrentUser))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void meReturnsTheAuthenticatedUserWithoutSensitiveData() throws Exception {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        when(getCurrentUser.execute(new GetCurrentUserQuery(user.getId()))).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/me").principal(authenticationOf(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.name").value("Arthur"))
                .andExpect(jsonPath("$.email").value("arthur@prumo.io"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meReturnsNotFoundWhenTheUserOfTheTokenNoLongerExists() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getCurrentUser.execute(new GetCurrentUserQuery(userId)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/me").principal(authenticationOf(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("User not found"));
    }

    private static UsernamePasswordAuthenticationToken authenticationOf(UUID userId) {
        return UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of());
    }
}
