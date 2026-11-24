package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.SubtaskProgress;
import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.SubtaskEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.SubtaskJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.SubtaskMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SubtaskPersistenceAdapter implements SubtaskRepositoryPort {

    private final SubtaskJpaRepository repository;

    public SubtaskPersistenceAdapter(SubtaskJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Subtask save(Subtask subtask) {
        return SubtaskMapper.toDomain(repository.saveAndFlush(SubtaskMapper.toEntity(subtask)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subtask> findByWorkspaceIdAndKey(UUID workspaceId, String key) {
        return repository.findByWorkspaceIdAndSubtaskKey(workspaceId, key).map(SubtaskMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subtask> findAllByStoryId(UUID storyId) {
        return repository.findByStoryIdOrderByPositionAsc(storyId).stream().map(SubtaskMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subtask> findAllByWorkspaceIdAndKeys(UUID workspaceId, Collection<String> keys) {
        return repository.findByWorkspaceIdAndSubtaskKeyIn(workspaceId, keys).stream().map(SubtaskMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, SubtaskProgress> progressByStoryIds(Collection<UUID> storyIds) {
        if (storyIds.isEmpty()) {
            return Map.of();
        }

        return repository.findByStoryIdIn(storyIds).stream()
                .collect(Collectors.groupingBy(
                        SubtaskEntity::getStoryId,
                        Collectors.collectingAndThen(Collectors.toList(), list -> new SubtaskProgress(
                                list.size(),
                                (int) list.stream().filter(entity -> entity.getStatus() == SubtaskStatus.DONE).count()
                        ))
                ));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
