package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.RecordSprintSnapshotsUseCase;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;

public class RecordSprintSnapshotsService implements RecordSprintSnapshotsUseCase {

    private final SprintRepositoryPort sprintRepository;
    private final SprintSnapshots snapshots;

    public RecordSprintSnapshotsService(SprintRepositoryPort sprintRepository, SprintSnapshots snapshots) {
        this.sprintRepository = sprintRepository;
        this.snapshots = snapshots;
    }

    @Override
    public void execute() {
        sprintRepository.findAllActive().forEach(snapshots::record);
    }
}
