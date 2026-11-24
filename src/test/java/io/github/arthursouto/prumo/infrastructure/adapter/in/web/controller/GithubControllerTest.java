package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.application.port.in.ConnectRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.DisconnectRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.GetGithubConnectionUseCase;
import io.github.arthursouto.prumo.application.port.in.ListAccessibleRepositoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.ListConnectedRepositoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.RemoveGithubTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.SaveGithubTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.SyncWorkspacePullRequestsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ConnectRepositoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.DisconnectRepositoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.RemoveGithubTokenCommand;
import io.github.arthursouto.prumo.application.port.in.command.SaveGithubTokenCommand;
import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.GlobalExceptionHandler;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SaveGithubTokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GithubControllerTest {

    @Mock
    private SaveGithubTokenUseCase saveToken;

    @Mock
    private GetGithubConnectionUseCase getConnection;

    @Mock
    private RemoveGithubTokenUseCase removeToken;

    @Mock
    private ListAccessibleRepositoriesUseCase listAccessible;

    @Mock
    private ListConnectedRepositoriesUseCase listConnected;

    @Mock
    private ConnectRepositoryUseCase connectRepository;

    @Mock
    private DisconnectRepositoryUseCase disconnectRepository;

    @Mock
    private SyncWorkspacePullRequestsUseCase sync;

    private MockMvc mockMvc;
    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GithubController(
                        saveToken, getConnection, removeToken, listAccessible,
                        listConnected, connectRepository, disconnectRepository, sync
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(userId.toString(), null);
    }

    @Test
    void savingATokenNeverEchoesItOrItsCipherText() throws Exception {
        when(saveToken.execute(new SaveGithubTokenCommand(userId, "ghp_secret")))
                .thenReturn(new GithubConnection(userId, "CIPHER-TEXT", "arthur", Instant.now()));

        mockMvc.perform(put("/api/v1/github/token").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"ghp_secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.login").value("arthur"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ghp_secret"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("CIPHER-TEXT"))));
    }

    @Test
    void theRequestObjectMasksTheTokenInLogs() {
        assertThat(new SaveGithubTokenRequest("ghp_secret").toString()).doesNotContain("ghp_secret");
    }

    @Test
    void aBlankTokenIsRejected() throws Exception {
        mockMvc.perform(put("/api/v1/github/token").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(saveToken);
    }

    @Test
    void connectionReportsWhetherATokenIsSavedWithoutRevealingIt() throws Exception {
        when(getConnection.execute(any()))
                .thenReturn(Optional.of(new GithubConnection(userId, "c", "arthur", Instant.now())))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/github/connection").principal(principal()))
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.tokenCipher").doesNotExist());
        mockMvc.perform(get("/api/v1/github/connection").principal(principal()))
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.login").doesNotExist());
    }

    @Test
    void removesTheToken() throws Exception {
        mockMvc.perform(delete("/api/v1/github/token").principal(principal())).andExpect(status().isNoContent());

        verify(removeToken).execute(new RemoveGithubTokenCommand(userId));
    }

    @Test
    void listsAccessibleRepositories() throws Exception {
        when(listAccessible.execute(any()))
                .thenReturn(List.of(new RemoteRepository("arthur", "prumo", "main", "https://github.com/arthur/prumo", true, null)));

        mockMvc.perform(get("/api/v1/github/repositories").principal(principal()))
                .andExpect(jsonPath("$[0].fullName").value("arthur/prumo"))
                .andExpect(jsonPath("$[0].isPrivate").value(true))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void connectsListsAndDisconnectsRepositories() throws Exception {
        ConnectedRepository repository = ConnectedRepository.connect(
                workspaceId, "arthur", "prumo", "main", "https://github.com/arthur/prumo", userId
        );
        when(connectRepository.execute(new ConnectRepositoryCommand(userId, workspaceId, "arthur", "prumo")))
                .thenReturn(repository);
        when(listConnected.execute(any())).thenReturn(List.of(repository));

        String base = "/api/v1/workspaces/" + workspaceId + "/repositories";
        mockMvc.perform(post(base).principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"arthur\",\"name\":\"prumo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(repository.getId().toString()));
        mockMvc.perform(get(base).principal(principal()))
                .andExpect(jsonPath("$[0].fullName").value("arthur/prumo"))
                .andExpect(jsonPath("$[0].lastSyncedAt").doesNotExist());
        mockMvc.perform(delete(base + "/" + repository.getId()).principal(principal()))
                .andExpect(status().isNoContent());

        verify(disconnectRepository).execute(new DisconnectRepositoryCommand(userId, workspaceId, repository.getId()));
    }

    @Test
    void ownerAndNameAreValidatedBeforeReachingGithub() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/repositories").principal(principal())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"owner\":\"../x\",\"name\":\"y\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(connectRepository);
    }

    @Test
    void syncReportsPerRepositoryFailuresWithoutFailingTheCall() throws Exception {
        ConnectedRepository repository = ConnectedRepository.connect(workspaceId, "a", "b", "main", "u", userId);
        repository.markFailed(Instant.now(), "RATE_LIMITED");
        when(sync.execute(any())).thenReturn(List.of(repository));

        mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/repositories/sync").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastSyncError").value("RATE_LIMITED"));
    }

    @Test
    void githubFailuresBecomeClearStatusesWithAStableCode() throws Exception {
        when(listAccessible.execute(any()))
                .thenThrow(new GithubIntegrationException(GithubFailure.INVALID_TOKEN, "The GitHub token is invalid or expired"))
                .thenThrow(new GithubIntegrationException(GithubFailure.RATE_LIMITED, "rate"))
                .thenThrow(new GithubIntegrationException(GithubFailure.NOT_FOUND, "nf"))
                .thenThrow(new GithubIntegrationException(GithubFailure.BRANCH_EXISTS, "be"))
                .thenThrow(new GithubIntegrationException(GithubFailure.UNAVAILABLE, "down"))
                .thenThrow(new GithubIntegrationException(GithubFailure.NOT_CONFIGURED, "key"))
                .thenThrow(new GithubIntegrationException(GithubFailure.MISSING_TOKEN, "tok"));

        expect(422, "INVALID_TOKEN");
        expect(429, "RATE_LIMITED");
        expect(404, "NOT_FOUND");
        expect(409, "BRANCH_EXISTS");
        expect(502, "UNAVAILABLE");
        expect(503, "NOT_CONFIGURED");
        expect(422, "MISSING_TOKEN");
    }

    private void expect(int status, String code) throws Exception {
        mockMvc.perform(get("/api/v1/github/repositories").principal(principal()))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").isString());
    }
}
