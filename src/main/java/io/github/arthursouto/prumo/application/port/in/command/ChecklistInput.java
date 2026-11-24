package io.github.arthursouto.prumo.application.port.in.command;

import java.util.UUID;

public record ChecklistInput(UUID id, String text, boolean done) {
}
