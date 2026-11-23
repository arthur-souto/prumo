package io.github.arthursouto.prumo.application.port.in.command;

public record RegisterUserCommand(String name, String email, String password) {
}
