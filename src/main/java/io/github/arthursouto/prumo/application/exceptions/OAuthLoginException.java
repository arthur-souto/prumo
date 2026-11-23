package io.github.arthursouto.prumo.application.exceptions;

public class OAuthLoginException extends RuntimeException {

    public enum Reason {
        EMAIL_UNAVAILABLE,
        EMAIL_ALREADY_REGISTERED,
        ACCOUNT_DISABLED,
        IDENTITY_ALREADY_LINKED,
        PROVIDER_ALREADY_LINKED,
        LINK_TOKEN_INVALID
    }

    private final Reason reason;

    public OAuthLoginException(Reason reason) {
        super("OAuth login rejected: " + reason);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
