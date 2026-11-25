package io.github.arthursouto.prumo.infrastructure.adapter.in.scheduler;

import io.github.arthursouto.prumo.application.port.in.RecordSprintSnapshotsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.sprints.snapshots", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SprintSnapshotRecorder {

    private final RecordSprintSnapshotsUseCase recordSnapshots;

    public SprintSnapshotRecorder(RecordSprintSnapshotsUseCase recordSnapshots) {
        this.recordSnapshots = recordSnapshots;
    }

    @Scheduled(
            fixedDelayString = "${app.sprints.snapshots.interval:1h}",
            initialDelayString = "${app.sprints.snapshots.initial-delay:30s}"
    )
    public void record() {
        recordSnapshots.execute();
    }
}
