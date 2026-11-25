package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.SprintSnapshotRepositoryPort;
import io.github.arthursouto.prumo.domain.model.sprint.SprintSnapshot;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.SprintSnapshotEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.SprintSnapshotJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class SprintSnapshotPersistenceAdapter implements SprintSnapshotRepositoryPort {

    private final SprintSnapshotJpaRepository repository;

    public SprintSnapshotPersistenceAdapter(SprintSnapshotJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void saveForDay(SprintSnapshot snapshot) {
        repository.saveForDay(
                snapshot.id(),
                snapshot.sprintId(),
                snapshot.date(),
                snapshot.totalPoints(),
                snapshot.remainingPoints(),
                snapshot.totalStories(),
                snapshot.remainingStories(),
                snapshot.recordedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintSnapshot> findBySprintId(UUID sprintId) {
        return repository.findBySprintIdOrderBySnapshotDateAsc(sprintId).stream()
                .map(SprintSnapshotPersistenceAdapter::toDomain)
                .toList();
    }

    private static SprintSnapshot toDomain(SprintSnapshotEntity entity) {
        return new SprintSnapshot(
                entity.getId(),
                entity.getSprintId(),
                entity.getSnapshotDate(),
                entity.getTotalPoints(),
                entity.getRemainingPoints(),
                entity.getTotalStories(),
                entity.getRemainingStories(),
                entity.getRecordedAt()
        );
    }
}
