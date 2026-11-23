package io.github.arthursouto.prumo.domain.exceptions;

/**
 * Base das exceções de domínio. Java puro, sem framework.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }

}
