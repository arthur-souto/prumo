package io.github.arthursouto.prumo.application.port.in.command;

public record AuthenticateUserCommand(String email, String password) {
}
