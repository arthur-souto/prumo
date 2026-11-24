package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.ActivityRepositoryPort;
import io.github.arthursouto.prumo.domain.model.activity.Activity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.ActivityEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.ActivityJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class ActivityPersistenceAdapter implements ActivityRepositoryPort {

    private final ActivityJpaRepository repository;

    public ActivityPersistenceAdapter(ActivityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Activity save(Activity activity) {
        return toDomain(repository.save(toEntity(activity)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> findByStoryId(UUID storyId, int limit) {
        return repository.findByStoryIdOrderByCreatedAtDesc(storyId, PageRequest.of(0, limit)).stream()
                .map(ActivityPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> findBySubtaskKey(UUID workspaceId, String subtaskKey, int limit) {
        return repository.findByWorkspaceIdAndSubtaskKeyOrderByCreatedAtDesc(workspaceId, subtaskKey, PageRequest.of(0, limit)).stream()
                .map(ActivityPersistenceAdapter::toDomain)
                .toList();
    }

    private static ActivityEntity toEntity(Activity activity) {
        ActivityEntity entity = new ActivityEntity();
        entity.setId(activity.id());
        entity.setWorkspaceId(activity.workspaceId());
        entity.setStoryId(activity.storyId());
        entity.setSubtaskKey(activity.subtaskKey());
        entity.setActorId(activity.actorId());
        entity.setSource(activity.source());
        entity.setType(activity.type());
        entity.setFromValue(activity.fromValue());
        entity.setToValue(activity.toValue());
        entity.setCreatedAt(activity.createdAt());
        return entity;
    }

    private static Activity toDomain(ActivityEntity entity) {
        return new Activity(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getStoryId(),
                entity.getSubtaskKey(),
                entity.getActorId(),
                entity.getSource(),
                entity.getType(),
                entity.getFromValue(),
                entity.getToValue(),
                entity.getCreatedAt()
        );
    }
}
