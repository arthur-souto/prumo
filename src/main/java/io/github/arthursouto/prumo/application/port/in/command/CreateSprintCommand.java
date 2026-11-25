package io.github.arthursouto.prumo.application.port.in.command;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSprintCommand(UUID userId, UUID workspaceId, String name, String goal, LocalDate startDate, LocalDate endDate) {
}
