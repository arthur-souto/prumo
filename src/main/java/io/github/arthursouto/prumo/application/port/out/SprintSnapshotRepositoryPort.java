package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.sprint.SprintSnapshot;

import java.util.List;
import java.util.UUID;

public interface SprintSnapshotRepositoryPort {

    void saveForDay(SprintSnapshot snapshot);

    List<SprintSnapshot> findBySprintId(UUID sprintId);
}
