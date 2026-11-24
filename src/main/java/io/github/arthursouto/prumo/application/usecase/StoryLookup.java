package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.Locale;
import java.util.UUID;

public class StoryLookup {

    private final WorkspaceAccess access;
    private final StoryRepositoryPort storyRepository;

    public StoryLookup(WorkspaceAccess access, StoryRepositoryPort storyRepository) {
        this.access = access;
        this.storyRepository = storyRepository;
    }

    public UserStory asMember(UUID userId, UUID workspaceId, String key) {
        access.member(userId, workspaceId);
        return find(workspaceId, key);
    }

    public UserStory asEditor(UUID userId, UUID workspaceId, String key) {
        access.editor(userId, workspaceId);
        return find(workspaceId, key);
    }

    private UserStory find(UUID workspaceId, String key) {
        return storyRepository.findByWorkspaceIdAndKey(workspaceId, key.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }
}
