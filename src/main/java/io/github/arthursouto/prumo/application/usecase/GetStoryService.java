package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetStoryQuery;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.Locale;

public class GetStoryService implements GetStoryUseCase {

    private final WorkspaceAccess access;
    private final StoryRepositoryPort storyRepository;

    public GetStoryService(WorkspaceAccess access, StoryRepositoryPort storyRepository) {
        this.access = access;
        this.storyRepository = storyRepository;
    }

    @Override
    public UserStory execute(GetStoryQuery query) {
        access.member(query.userId(), query.workspaceId());

        return storyRepository.findByWorkspaceIdAndKey(query.workspaceId(), query.key().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }
}
