package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SubtaskLookup {

    private final WorkspaceAccess access;
    private final SubtaskRepositoryPort subtaskRepository;
    private final StoryRepositoryPort storyRepository;

    public SubtaskLookup(WorkspaceAccess access, SubtaskRepositoryPort subtaskRepository, StoryRepositoryPort storyRepository) {
        this.access = access;
        this.subtaskRepository = subtaskRepository;
        this.storyRepository = storyRepository;
    }

    public Workspace workspace(UUID userId, UUID workspaceId) {
        return access.member(userId, workspaceId);
    }

    public Subtask asMember(UUID userId, UUID workspaceId, String key) {
        access.member(userId, workspaceId);

        return subtaskRepository.findByWorkspaceIdAndKey(workspaceId, key.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));
    }

    public UserStory storyOf(Subtask subtask) {
        return storyRepository.findById(subtask.getStoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }

    public SubtaskView view(Subtask subtask) {
        return view(subtask, storyOf(subtask));
    }

    public SubtaskView view(Subtask subtask, UserStory story) {
        List<PullRequest> pullRequests = story.getPullRequests().stream()
                .filter(pullRequest -> subtask.getId().equals(pullRequest.subtaskId()))
                .toList();

        return new SubtaskView(subtask, story.getKey(), pullRequests);
    }
}
