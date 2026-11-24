package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.SubtaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "A subtask of a user story. It has its own key and status, and at most one branch")
public record SubtaskResponse(
        UUID id,
        String key,
        String storyKey,
        String title,
        String description,
        SubtaskStatus status,
        int position,
        String branchName,
        List<StoryResponse.PullRequestResponse> pullRequests,
        Instant createdAt,
        Instant updatedAt
) {

    public static SubtaskResponse from(SubtaskView view) {
        Subtask subtask = view.subtask();

        return new SubtaskResponse(
                subtask.getId(),
                subtask.getKey(),
                view.storyKey(),
                subtask.getTitle(),
                subtask.getDescription(),
                subtask.getStatus(),
                subtask.getPosition(),
                subtask.getBranchName(),
                view.pullRequests().stream().map(StoryResponse.PullRequestResponse::from).toList(),
                subtask.getCreatedAt(),
                subtask.getUpdatedAt()
        );
    }
}
