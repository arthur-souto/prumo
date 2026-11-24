package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.RemoveGithubTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.command.RemoveGithubTokenCommand;
import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;

public class RemoveGithubTokenService implements RemoveGithubTokenUseCase {

    private final GithubConnectionRepositoryPort connectionRepository;

    public RemoveGithubTokenService(GithubConnectionRepositoryPort connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Override
    public void execute(RemoveGithubTokenCommand command) {
        connectionRepository.deleteByUserId(command.userId());
    }
}
