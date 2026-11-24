package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetGithubConnectionUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetGithubConnectionQuery;
import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;

import java.util.Optional;

public class GetGithubConnectionService implements GetGithubConnectionUseCase {

    private final GithubConnectionRepositoryPort connectionRepository;

    public GetGithubConnectionService(GithubConnectionRepositoryPort connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Optional<GithubConnection> execute(GetGithubConnectionQuery query) {
        return connectionRepository.findByUserId(query.userId());
    }
}
