package io.github.arthursouto.prumo.domain.model.story;

/** Who made a status or column change: a person on the board, or the GitHub pull request sync. */
public enum ChangeSource {
    USER,
    GITHUB
}
