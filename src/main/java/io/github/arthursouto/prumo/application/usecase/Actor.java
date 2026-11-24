package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.domain.model.story.ChangeSource;

import java.util.UUID;

/** Who is behind a change: a person (with their id) or the GitHub pull request sync (no user). */
public record Actor(ChangeSource source, UUID userId) {

    public static final Actor GITHUB = new Actor(ChangeSource.GITHUB, null);

    public static Actor user(UUID userId) {
        return new Actor(ChangeSource.USER, userId);
    }
}
