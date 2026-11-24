package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.TokenCipherPort;

import java.util.UUID;

public class GithubTokens {

    private final GithubConnectionRepositoryPort connectionRepository;
    private final TokenCipherPort cipher;

    public GithubTokens(GithubConnectionRepositoryPort connectionRepository, TokenCipherPort cipher) {
        this.connectionRepository = connectionRepository;
        this.cipher = cipher;
    }

    public String of(UUID userId) {
        return connectionRepository.findByUserId(userId)
                .map(connection -> cipher.decrypt(connection.tokenCipher()))
                .orElseThrow(() -> new GithubIntegrationException(
                        GithubFailure.MISSING_TOKEN,
                        "Connect your GitHub token first"
                ));
    }
}
