package io.github.arthursouto.prumo.application.port.in.result;

import java.time.LocalDate;

public record BurndownDay(
        LocalDate date,
        Integer totalPoints,
        Integer remainingPoints,
        Integer totalStories,
        Integer remainingStories,
        double idealPoints
) {
}
