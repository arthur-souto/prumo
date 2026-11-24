package io.github.arthursouto.prumo.domain.model.story;

import java.util.UUID;

public record ChecklistItem(UUID id, String text, boolean done) {

    public static ChecklistItem create(String text, boolean done) {
        return new ChecklistItem(UUID.randomUUID(), text, done);
    }

    public ChecklistItem withDone(boolean done) {
        return new ChecklistItem(id, text, done);
    }
}
