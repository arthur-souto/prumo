package io.github.arthursouto.prumo.domain.exceptions;

/**
 * O recurso pedido não existe (HTTP 404).
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }

}
