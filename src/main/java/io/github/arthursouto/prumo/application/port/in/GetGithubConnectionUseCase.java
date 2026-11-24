package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetGithubConnectionQuery;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import java.util.Optional;

public interface GetGithubConnectionUseCase {

    Optional<GithubConnection> execute(GetGithubConnectionQuery command);
}
