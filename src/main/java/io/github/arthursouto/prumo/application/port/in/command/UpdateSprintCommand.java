package io.github.arthursouto.prumo.application.port.in.command;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSprintCommand(
        UUID userId,
        UUID workspaceId,
        UUID sprintId,
        String name,
        String goal,
        LocalDate startDate,
        LocalDate endDate
) {
}
