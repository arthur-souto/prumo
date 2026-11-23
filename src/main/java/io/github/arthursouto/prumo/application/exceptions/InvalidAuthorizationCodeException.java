package io.github.arthursouto.prumo.application.exceptions;

public class InvalidAuthorizationCodeException extends RuntimeException {

    public InvalidAuthorizationCodeException() {
        super("Invalid authorization code");
    }
}
