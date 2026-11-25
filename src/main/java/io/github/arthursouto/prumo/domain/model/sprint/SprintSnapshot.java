package io.github.arthursouto.prumo.domain.model.sprint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SprintSnapshot(
        UUID id,
        UUID sprintId,
        LocalDate date,
        int totalPoints,
        int remainingPoints,
        int totalStories,
        int remainingStories,
        Instant recordedAt
) {

    public static SprintSnapshot of(UUID sprintId,
                                    LocalDate date,
                                    int totalPoints,
                                    int remainingPoints,
                                    int totalStories,
                                    int remainingStories,
                                    Instant recordedAt) {
        return new SprintSnapshot(UUID.randomUUID(), sprintId, date, totalPoints, remainingPoints, totalStories, remainingStories, recordedAt);
    }
}
