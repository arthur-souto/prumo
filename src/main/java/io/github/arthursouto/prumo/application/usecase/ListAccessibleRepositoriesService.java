package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListAccessibleRepositoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListAccessibleRepositoriesQuery;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;

import java.util.List;

public class ListAccessibleRepositoriesService implements ListAccessibleRepositoriesUseCase {

    private final GithubTokens tokens;
    private final GithubPort github;

    public ListAccessibleRepositoriesService(GithubTokens tokens, GithubPort github) {
        this.tokens = tokens;
        this.github = github;
    }

    @Override
    public List<RemoteRepository> execute(ListAccessibleRepositoriesQuery query) {
        return github.listRepositories(tokens.of(query.userId()));
    }
}
