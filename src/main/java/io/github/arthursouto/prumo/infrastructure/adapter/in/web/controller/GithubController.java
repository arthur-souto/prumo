package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

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
import io.github.arthursouto.prumo.application.port.in.command.GetGithubConnectionQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListAccessibleRepositoriesQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListConnectedRepositoriesQuery;
import io.github.arthursouto.prumo.application.port.in.command.RemoveGithubTokenCommand;
import io.github.arthursouto.prumo.application.port.in.command.SaveGithubTokenCommand;
import io.github.arthursouto.prumo.application.port.in.command.SyncWorkspacePullRequestsCommand;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.GithubApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ConnectRepositoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.GithubConnectionResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.RepositoryResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SaveGithubTokenRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class GithubController implements GithubApi {

    private final SaveGithubTokenUseCase saveToken;
    private final GetGithubConnectionUseCase getConnection;
    private final RemoveGithubTokenUseCase removeToken;
    private final ListAccessibleRepositoriesUseCase listAccessible;
    private final ListConnectedRepositoriesUseCase listConnected;
    private final ConnectRepositoryUseCase connectRepository;
    private final DisconnectRepositoryUseCase disconnectRepository;
    private final SyncWorkspacePullRequestsUseCase sync;

    public GithubController(SaveGithubTokenUseCase saveToken,
                            GetGithubConnectionUseCase getConnection,
                            RemoveGithubTokenUseCase removeToken,
                            ListAccessibleRepositoriesUseCase listAccessible,
                            ListConnectedRepositoriesUseCase listConnected,
                            ConnectRepositoryUseCase connectRepository,
                            DisconnectRepositoryUseCase disconnectRepository,
                            SyncWorkspacePullRequestsUseCase sync) {
        this.saveToken = saveToken;
        this.getConnection = getConnection;
        this.removeToken = removeToken;
        this.listAccessible = listAccessible;
        this.listConnected = listConnected;
        this.connectRepository = connectRepository;
        this.disconnectRepository = disconnectRepository;
        this.sync = sync;
    }

    @Override
    public GithubConnectionResponse saveToken(Authentication authentication, SaveGithubTokenRequest request) {
        return GithubConnectionResponse.from(
                saveToken.execute(new SaveGithubTokenCommand(userId(authentication), request.token()))
        );
    }

    @Override
    public GithubConnectionResponse connection(Authentication authentication) {
        return getConnection.execute(new GetGithubConnectionQuery(userId(authentication)))
                .map(GithubConnectionResponse::from)
                .orElseGet(GithubConnectionResponse::none);
    }

    @Override
    public ResponseEntity<Void> removeToken(Authentication authentication) {
        removeToken.execute(new RemoveGithubTokenCommand(userId(authentication)));

        return ResponseEntity.noContent().build();
    }

    @Override
    public List<RepositoryResponse> accessibleRepositories(Authentication authentication) {
        return listAccessible.execute(new ListAccessibleRepositoriesQuery(userId(authentication))).stream()
                .map(RepositoryResponse::from)
                .toList();
    }

    @Override
    public List<RepositoryResponse> repositories(Authentication authentication, UUID workspaceId) {
        return listConnected.execute(new ListConnectedRepositoriesQuery(userId(authentication), workspaceId)).stream()
                .map(RepositoryResponse::from)
                .toList();
    }

    @Override
    public ResponseEntity<RepositoryResponse> connect(Authentication authentication,
                                                      UUID workspaceId,
                                                      ConnectRepositoryRequest request) {
        return ResponseEntity.status(201).body(RepositoryResponse.from(connectRepository.execute(
                new ConnectRepositoryCommand(userId(authentication), workspaceId, request.owner(), request.name())
        )));
    }

    @Override
    public ResponseEntity<Void> disconnect(Authentication authentication, UUID workspaceId, UUID repositoryId) {
        disconnectRepository.execute(new DisconnectRepositoryCommand(userId(authentication), workspaceId, repositoryId));

        return ResponseEntity.noContent().build();
    }

    @Override
    public List<RepositoryResponse> sync(Authentication authentication, UUID workspaceId) {
        return sync.execute(new SyncWorkspacePullRequestsCommand(userId(authentication), workspaceId)).stream()
                .map(RepositoryResponse::from)
                .toList();
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
