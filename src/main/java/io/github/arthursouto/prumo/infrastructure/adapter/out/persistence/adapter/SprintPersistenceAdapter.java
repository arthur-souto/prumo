package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.query.SprintSearchCriteria;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintState;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.SprintEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.SprintJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class SprintPersistenceAdapter implements SprintRepositoryPort {

    private final SprintJpaRepository repository;

    public SprintPersistenceAdapter(SprintJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Sprint save(Sprint sprint) {
        return toDomain(repository.saveAndFlush(toEntity(sprint)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sprint> findById(UUID id) {
        return repository.findById(id).map(SprintPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Sprint> search(SprintSearchCriteria criteria) {
        String text = criteria.text() == null ? "" : criteria.text().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        Page<SprintEntity> page = repository.search(
                criteria.workspaceId(),
                criteria.states(),
                "%" + text + "%",
                PageRequest.of(criteria.page(), criteria.size())
        );

        return new PageResult<>(page.getContent().stream().map(SprintPersistenceAdapter::toDomain).toList(),
                criteria.page(), criteria.size(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sprint> findActive(UUID workspaceId) {
        return repository.findByWorkspaceIdAndState(workspaceId, SprintState.ACTIVE).map(SprintPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public void delete(Sprint sprint) {
        repository.deleteById(sprint.getId());
        repository.flush();
    }

    private static SprintEntity toEntity(Sprint sprint) {
        SprintEntity entity = new SprintEntity();
        entity.setId(sprint.getId());
        entity.setWorkspaceId(sprint.getWorkspaceId());
        entity.setName(sprint.getName());
        entity.setGoal(sprint.getGoal());
        entity.setStartDate(sprint.getStartDate());
        entity.setEndDate(sprint.getEndDate());
        entity.setState(sprint.getState());
        entity.setCommittedPoints(sprint.getCommittedPoints());
        entity.setCompletedPoints(sprint.getCompletedPoints());
        entity.setStartedAt(sprint.getStartedAt());
        entity.setClosedAt(sprint.getClosedAt());
        entity.setCreatedAt(sprint.getCreatedAt());
        entity.setUpdatedAt(sprint.getUpdatedAt());
        return entity;
    }

    private static Sprint toDomain(SprintEntity entity) {
        return Sprint.restore(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getName(),
                entity.getGoal(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getState(),
                entity.getCommittedPoints(),
                entity.getCompletedPoints(),
                entity.getStartedAt(),
                entity.getClosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
