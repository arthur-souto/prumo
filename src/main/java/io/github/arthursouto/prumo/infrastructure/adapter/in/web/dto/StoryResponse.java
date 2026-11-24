package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.domain.model.story.ChecklistItem;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.PullRequestState;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "A user story")
public record StoryResponse(
        UUID id,
        String key,
        String title,
        String description,
        StoryPriority priority,
        Integer storyPoints,
        UUID columnId,
        List<String> labels,
        List<ChecklistResponse> subtasks,
        List<ChecklistResponse> acceptanceCriteria,
        UUID repositoryId,
        String branchName,
        List<PullRequestResponse> pullRequests,
        Instant createdAt,
        Instant updatedAt
) {

    public static StoryResponse from(UserStory story) {
        return new StoryResponse(
                story.getId(),
                story.getKey(),
                story.getTitle(),
                story.getDescription(),
                story.getPriority(),
                story.getStoryPoints(),
                story.getColumnId(),
                story.getLabels(),
                story.getSubtasks().stream().map(ChecklistResponse::from).toList(),
                story.getAcceptanceCriteria().stream().map(ChecklistResponse::from).toList(),
                story.getRepositoryId(),
                story.getBranchName(),
                story.getPullRequests().stream().map(PullRequestResponse::from).toList(),
                story.getCreatedAt(),
                story.getUpdatedAt()
        );
    }

    public record ChecklistResponse(UUID id, String text, boolean done) {

        static ChecklistResponse from(ChecklistItem item) {
            return new ChecklistResponse(item.id(), item.text(), item.done());
        }
    }

    public record PullRequestResponse(
            int number,
            String title,
            PullRequestState state,
            String url,
            String branch,
            UUID repositoryId
    ) {

        static PullRequestResponse from(PullRequest pullRequest) {
            return new PullRequestResponse(
                    pullRequest.number(),
                    pullRequest.title(),
                    pullRequest.state(),
                    pullRequest.url(),
                    pullRequest.branch(),
                    pullRequest.repositoryId()
            );
        }
    }
}
