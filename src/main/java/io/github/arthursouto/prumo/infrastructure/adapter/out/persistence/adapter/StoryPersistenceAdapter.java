package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.PullRequestEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.StoryChecklistItemEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.StoryLabelEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.UserStoryEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.PullRequestJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.StoryChecklistItemJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.StoryLabelJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.UserStoryJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.StoryMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StoryPersistenceAdapter implements StoryRepositoryPort {

    private static final Comparator<UserStory> BY_RANK = Comparator
            .comparing(UserStory::getRank)
            .thenComparing(UserStory::getId);

    private final UserStoryJpaRepository storyRepository;
    private final StoryLabelJpaRepository labelRepository;
    private final StoryChecklistItemJpaRepository itemRepository;
    private final PullRequestJpaRepository pullRequestRepository;

    public StoryPersistenceAdapter(UserStoryJpaRepository storyRepository,
                                   StoryLabelJpaRepository labelRepository,
                                   StoryChecklistItemJpaRepository itemRepository,
                                   PullRequestJpaRepository pullRequestRepository) {
        this.storyRepository = storyRepository;
        this.labelRepository = labelRepository;
        this.itemRepository = itemRepository;
        this.pullRequestRepository = pullRequestRepository;
    }

    @Override
    @Transactional
    public UserStory save(UserStory story) {
        return saveAll(List.of(story)).get(0);
    }

    @Override
    @Transactional
    public List<UserStory> saveAll(List<UserStory> stories) {
        List<UserStoryEntity> saved = storyRepository.saveAllAndFlush(
                stories.stream().map(StoryMapper::toEntity).toList()
        );

        for (UserStory story : stories) {
            labelRepository.deleteByStoryId(story.getId());
            itemRepository.deleteByStoryId(story.getId());
            labelRepository.saveAll(StoryMapper.labelEntities(story));
            itemRepository.saveAll(StoryMapper.itemEntities(story));
        }

        labelRepository.flush();

        return hydrate(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserStory> findById(UUID id) {
        return storyRepository.findById(id).map(entity -> hydrate(List.of(entity)).get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserStory> findByWorkspaceIdAndKey(UUID workspaceId, String key) {
        return storyRepository.findByWorkspaceIdAndStoryKey(workspaceId, key)
                .map(entity -> hydrate(List.of(entity)).get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStory> findAllByWorkspaceId(UUID workspaceId) {
        return hydrate(storyRepository.findByWorkspaceId(workspaceId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStory> findAllByColumnId(UUID columnId) {
        return hydrate(storyRepository.findByColumnId(columnId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStory> findAllByWorkspaceIdAndKeys(UUID workspaceId, Collection<String> keys) {
        return hydrate(storyRepository.findByWorkspaceIdAndStoryKeyIn(workspaceId, keys));
    }

    @Override
    public long countByWorkspaceId(UUID workspaceId) {
        return storyRepository.countByWorkspaceId(workspaceId);
    }

    @Override
    public void deleteById(UUID id) {
        storyRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PullRequest> findPullRequest(UUID storyId, UUID repositoryId, int number) {
        return pullRequestRepository.findByStoryIdAndRepositoryIdAndNumber(storyId, repositoryId, number)
                .map(StoryMapper::toDomain);
    }

    @Override
    @Transactional
    public PullRequest savePullRequest(PullRequest pullRequest) {
        return StoryMapper.toDomain(pullRequestRepository.save(StoryMapper.toEntity(pullRequest)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PullRequest> findPullRequestsBySubtaskId(UUID subtaskId) {
        return pullRequestRepository.findBySubtaskId(subtaskId).stream().map(StoryMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PullRequest> findPullRequestsOfStoryWithoutSubtask(UUID storyId) {
        return pullRequestRepository.findByStoryIdAndSubtaskIdIsNull(storyId).stream().map(StoryMapper::toDomain).toList();
    }

    private List<UserStory> hydrate(List<UserStoryEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = entities.stream().map(UserStoryEntity::getId).toList();
        Map<UUID, List<StoryLabelEntity>> labels = labelRepository.findByStoryIdIn(ids).stream()
                .collect(Collectors.groupingBy(StoryLabelEntity::getStoryId));
        Map<UUID, List<StoryChecklistItemEntity>> items = itemRepository.findByStoryIdIn(ids).stream()
                .collect(Collectors.groupingBy(StoryChecklistItemEntity::getStoryId));
        Map<UUID, List<PullRequestEntity>> pullRequests = pullRequestRepository.findByStoryIdIn(ids).stream()
                .collect(Collectors.groupingBy(PullRequestEntity::getStoryId));

        return entities.stream()
                .map(entity -> StoryMapper.toDomain(
                        entity,
                        labels.getOrDefault(entity.getId(), List.of()),
                        items.getOrDefault(entity.getId(), List.of()),
                        pullRequests.getOrDefault(entity.getId(), List.of())
                ))
                .sorted(BY_RANK)
                .toList();
    }
}
