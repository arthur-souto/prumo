package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.ChecklistInput;
import io.github.arthursouto.prumo.domain.model.story.ChecklistItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class Checklists {

    private Checklists() {
    }

    static List<ChecklistItem> items(List<ChecklistInput> inputs, List<ChecklistItem> existing) {
        if (inputs == null) {
            return List.of();
        }

        Map<UUID, ChecklistItem> known = new LinkedHashMap<>();
        existing.forEach(item -> known.put(item.id(), item));

        return inputs.stream()
                .filter(input -> input.text() != null && !input.text().isBlank())
                .map(input -> {
                    String text = input.text().strip();

                    if (input.id() != null && known.containsKey(input.id())) {
                        return new ChecklistItem(input.id(), text, input.done());
                    }

                    return ChecklistItem.create(text, input.done());
                })
                .toList();
    }

    static List<String> labels(List<String> labels) {
        if (labels == null) {
            return List.of();
        }

        Map<String, String> unique = new LinkedHashMap<>();

        labels.stream()
                .filter(label -> label != null && !label.isBlank())
                .map(String::strip)
                .forEach(label -> unique.putIfAbsent(label.toLowerCase(Locale.ROOT), label));

        return List.copyOf(unique.values());
    }

    static String text(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
