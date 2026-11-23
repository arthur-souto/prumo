package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidLoginRequestException extends AuthenticationException {

    public InvalidLoginRequestException() {
        super("Invalid login request");
    }
}
