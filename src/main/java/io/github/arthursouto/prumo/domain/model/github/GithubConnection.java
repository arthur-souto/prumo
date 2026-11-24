package io.github.arthursouto.prumo.domain.model.github;

import java.time.Instant;
import java.util.UUID;

public record GithubConnection(UUID userId, String tokenCipher, String login, Instant createdAt) {
}
