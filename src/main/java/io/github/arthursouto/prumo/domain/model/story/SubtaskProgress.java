package io.github.arthursouto.prumo.domain.model.story;

/** How many subtasks a story has and how many of them are done. */
public record SubtaskProgress(int total, int done) {
}
