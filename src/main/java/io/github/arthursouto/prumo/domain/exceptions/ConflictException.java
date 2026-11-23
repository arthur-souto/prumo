package io.github.arthursouto.prumo.domain.exceptions;

/**
 * A operação conflita com o estado atual, como um registro duplicado (HTTP 409).
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }

}
