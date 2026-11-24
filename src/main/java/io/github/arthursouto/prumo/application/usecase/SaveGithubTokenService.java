package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.SaveGithubTokenUseCase;
import io.github.arthursouto.prumo.application.port.in.command.SaveGithubTokenCommand;
import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.GithubPort;
import io.github.arthursouto.prumo.application.port.out.TokenCipherPort;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;

import java.time.Clock;

public class SaveGithubTokenService implements SaveGithubTokenUseCase {

    private final GithubPort github;
    private final GithubConnectionRepositoryPort connectionRepository;
    private final TokenCipherPort cipher;
    private final Clock clock;

    public SaveGithubTokenService(GithubPort github,
                                  GithubConnectionRepositoryPort connectionRepository,
                                  TokenCipherPort cipher,
                                  Clock clock) {
        this.github = github;
        this.connectionRepository = connectionRepository;
        this.cipher = cipher;
        this.clock = clock;
    }

    @Override
    public GithubConnection execute(SaveGithubTokenCommand command) {
        String token = command.token().strip();
        String login = github.authenticate(token);

        return connectionRepository.save(new GithubConnection(
                command.userId(),
                cipher.encrypt(token),
                login,
                clock.instant()
        ));
    }
}
