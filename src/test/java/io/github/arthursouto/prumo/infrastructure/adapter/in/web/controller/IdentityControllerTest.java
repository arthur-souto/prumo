package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.CreateLinkTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.GetUserIdentitiesUseCase;
import io.github.arthursouto.prumo.application.port.in.UnlinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateLinkTokenCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetUserIdentitiesQuery;
import io.github.arthursouto.prumo.application.port.in.command.UnlinkOAuthIdentityCommand;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IdentityControllerTest {

    @Mock
    private GetUserIdentitiesUseCase getUserIdentities;

    @Mock
    private CreateLinkTokenUseCase createLinkToken;

    @Mock
    private UnlinkOAuthIdentityUseCase unlinkOAuthIdentity;

    private MockMvc mockMvc;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new IdentityController(getUserIdentities, createLinkToken, unlinkOAuthIdentity))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listReturnsTheLinkedProviders() throws Exception {
        when(getUserIdentities.execute(new GetUserIdentitiesQuery(userId))).thenReturn(List.of(
                UserIdentity.link(userId, AuthProvider.GITHUB, "42", Instant.parse("2026-01-01T00:00:00Z"))
        ));

        mockMvc.perform(get("/api/v1/users/me/identities").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].provider").value("GITHUB"))
                .andExpect(jsonPath("$[0].providerId").doesNotExist());
    }

    @Test
    void startLinkReturnsTheAuthorizationPathWithTheToken() throws Exception {
        when(createLinkToken.execute(new CreateLinkTokenCommand(userId))).thenReturn("tok");

        mockMvc.perform(post("/api/v1/users/me/identities/GITHUB/link").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.authorization_path").value("/oauth2/authorization/github?link_token=tok"));
    }

    @Test
    void unlinkReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/identities/github").principal(principal()))
                .andExpect(status().isNoContent());

        verify(unlinkOAuthIdentity).execute(new UnlinkOAuthIdentityCommand(userId, AuthProvider.GITHUB));
    }

    @Test
    void unlinkReturnsUnprocessableWhenItIsTheOnlyLoginMethod() throws Exception {
        doThrow(new BusinessRuleException("Set a password before unlinking the only login method"))
                .when(unlinkOAuthIdentity).execute(new UnlinkOAuthIdentityCommand(userId, AuthProvider.GITHUB));

        mockMvc.perform(delete("/api/v1/users/me/identities/GITHUB").principal(principal()))
                .andExpect(status().isUnprocessableEntity());
    }

    private UsernamePasswordAuthenticationToken principal() {
        return UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of());
    }

    @Test
    void unknownProviderIsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/identities/gitlab/link").principal(principal()))
                .andExpect(status().isNotFound());
    }
}
