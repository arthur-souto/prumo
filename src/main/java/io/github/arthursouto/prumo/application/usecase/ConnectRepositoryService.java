package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ConnectRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ConnectRepositoryCommand;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;

public class ConnectRepositoryService implements ConnectRepositoryUseCase {

    private final WorkspaceAccess access;
    private final GithubTokens tokens;
    private final GithubPort github;
    private final RepositoryConnector connector;

    public ConnectRepositoryService(WorkspaceAccess access,
                                    GithubTokens tokens,
                                    GithubPort github,
                                    RepositoryConnector connector) {
        this.access = access;
        this.tokens = tokens;
        this.github = github;
        this.connector = connector;
    }

    @Override
    public ConnectedRepository execute(ConnectRepositoryCommand command) {
        access.editor(command.userId(), command.workspaceId());

        return connector.connect(
                command.workspaceId(),
                command.userId(),
                github.getRepository(tokens.of(command.userId()), command.owner().strip(), command.name().strip())
        );
    }
}
