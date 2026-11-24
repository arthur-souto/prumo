package io.github.arthursouto.prumo.application.exceptions;

public enum GithubFailure {
    MISSING_TOKEN,
    INVALID_TOKEN,
    PERMISSION_DENIED,
    RATE_LIMITED,
    NOT_FOUND,
    BRANCH_EXISTS,
    REPOSITORY_EXISTS,
    UNAVAILABLE,
    NOT_CONFIGURED
}
