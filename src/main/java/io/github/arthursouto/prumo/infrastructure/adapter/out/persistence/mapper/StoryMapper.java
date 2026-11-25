package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper;

import io.github.arthursouto.prumo.domain.model.story.ChecklistItem;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.PullRequestEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.StoryChecklistItemEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.StoryLabelEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.UserStoryEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class StoryMapper {

    public static final String CRITERION = "CRITERION";

    private StoryMapper() {
    }

    public static UserStoryEntity toEntity(UserStory story) {
        UserStoryEntity entity = new UserStoryEntity();
        entity.setId(story.getId());
        entity.setWorkspaceId(story.getWorkspaceId());
        entity.setNumber(story.getNumber());
        entity.setStoryKey(story.getKey());
        entity.setTitle(story.getTitle());
        entity.setDescription(story.getDescription());
        entity.setPriority(story.getPriority());
        entity.setStoryPoints(story.getStoryPoints());
        entity.setColumnId(story.getColumnId());
        entity.setColumnSource(story.getColumnSource());
        entity.setSortRank(story.getRank());
        entity.setRepositoryId(story.getRepositoryId());
        entity.setAssigneeId(story.getAssigneeId());
        entity.setSprintId(story.getSprintId());
        entity.setCreatedAt(story.getCreatedAt());
        entity.setUpdatedAt(story.getUpdatedAt());
        return entity;
    }

    public static List<StoryLabelEntity> labelEntities(UserStory story) {
        List<StoryLabelEntity> entities = new ArrayList<>();

        for (int position = 0; position < story.getLabels().size(); position++) {
            StoryLabelEntity entity = new StoryLabelEntity();
            entity.setId(UUID.randomUUID());
            entity.setStoryId(story.getId());
            entity.setLabel(story.getLabels().get(position));
            entity.setPosition(position);
            entities.add(entity);
        }

        return entities;
    }

    public static List<StoryChecklistItemEntity> itemEntities(UserStory story) {
        List<StoryChecklistItemEntity> entities = new ArrayList<>();
        append(entities, story.getId(), CRITERION, story.getAcceptanceCriteria());
        return entities;
    }

    public static PullRequestEntity toEntity(PullRequest pullRequest) {
        PullRequestEntity entity = new PullRequestEntity();
        entity.setId(pullRequest.id());
        entity.setStoryId(pullRequest.storyId());
        entity.setSubtaskId(pullRequest.subtaskId());
        entity.setRepositoryId(pullRequest.repositoryId());
        entity.setNumber(pullRequest.number());
        entity.setTitle(pullRequest.title());
        entity.setState(pullRequest.state());
        entity.setUrl(pullRequest.url());
        entity.setBranch(pullRequest.branch());
        entity.setUpdatedAt(pullRequest.updatedAt());
        return entity;
    }

    public static PullRequest toDomain(PullRequestEntity entity) {
        return new PullRequest(
                entity.getId(),
                entity.getStoryId(),
                entity.getSubtaskId(),
                entity.getRepositoryId(),
                entity.getNumber(),
                entity.getTitle(),
                entity.getState(),
                entity.getUrl(),
                entity.getBranch(),
                entity.getUpdatedAt()
        );
    }

    public static UserStory toDomain(UserStoryEntity entity,
                                     List<StoryLabelEntity> labels,
                                     List<StoryChecklistItemEntity> items,
                                     List<PullRequestEntity> pullRequests) {
        return UserStory.restore(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getNumber(),
                entity.getStoryKey(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getStoryPoints(),
                entity.getColumnId(),
                entity.getSortRank(),
                labels.stream()
                        .sorted(Comparator.comparingInt(StoryLabelEntity::getPosition))
                        .map(StoryLabelEntity::getLabel)
                        .toList(),
                checklist(items, CRITERION),
                entity.getRepositoryId(),
                pullRequests.stream()
                        .sorted(Comparator.comparing(PullRequestEntity::getUpdatedAt).reversed())
                        .map(StoryMapper::toDomain)
                        .toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        ).withAssignee(entity.getAssigneeId()).withSprint(entity.getSprintId()).withColumnSource(entity.getColumnSource());
    }

    private static void append(List<StoryChecklistItemEntity> target, UUID storyId, String kind, List<ChecklistItem> items) {
        for (int position = 0; position < items.size(); position++) {
            StoryChecklistItemEntity entity = new StoryChecklistItemEntity();
            entity.setId(items.get(position).id());
            entity.setStoryId(storyId);
            entity.setKind(kind);
            entity.setText(items.get(position).text());
            entity.setDone(items.get(position).done());
            entity.setPosition(position);
            target.add(entity);
        }
    }

    private static List<ChecklistItem> checklist(List<StoryChecklistItemEntity> items, String kind) {
        return items.stream()
                .filter(item -> item.getKind().equals(kind))
                .sorted(Comparator.comparingInt(StoryChecklistItemEntity::getPosition))
                .map(item -> new ChecklistItem(item.getId(), item.getText(), item.isDone()))
                .toList();
    }
}
