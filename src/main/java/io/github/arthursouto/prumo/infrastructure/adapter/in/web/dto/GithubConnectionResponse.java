package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Whether the user has a GitHub token saved. The token itself is never returned")
public record GithubConnectionResponse(boolean connected, String login, Instant connectedAt) {

    public static GithubConnectionResponse from(GithubConnection connection) {
        return new GithubConnectionResponse(true, connection.login(), connection.createdAt());
    }

    public static GithubConnectionResponse none() {
        return new GithubConnectionResponse(false, null, null);
    }
}
