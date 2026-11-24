package io.github.arthursouto.prumo.application.exceptions;

public class KeyPrefixTakenException extends RuntimeException {

    public KeyPrefixTakenException() {
        super("Story key prefix already taken");
    }
}
