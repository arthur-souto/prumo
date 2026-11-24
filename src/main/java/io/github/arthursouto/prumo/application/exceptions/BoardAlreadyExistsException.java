package io.github.arthursouto.prumo.application.exceptions;

public class BoardAlreadyExistsException extends RuntimeException {

    public BoardAlreadyExistsException() {
        super("Board already exists");
    }
}
