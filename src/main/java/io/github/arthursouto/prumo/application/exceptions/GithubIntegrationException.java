package io.github.arthursouto.prumo.application.exceptions;

public class GithubIntegrationException extends RuntimeException {

    private final GithubFailure reason;

    public GithubIntegrationException(GithubFailure reason, String message) {
        super(message);
        this.reason = reason;
    }

    public GithubFailure getReason() {
        return reason;
    }
}
