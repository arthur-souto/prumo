package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.SuggestBranchNameUseCase;
import io.github.arthursouto.prumo.application.port.in.command.SuggestBranchNameQuery;
import io.github.arthursouto.prumo.domain.model.github.BranchNames;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class SuggestBranchNameService implements SuggestBranchNameUseCase {

    private final StoryLookup stories;

    public SuggestBranchNameService(StoryLookup stories) {
        this.stories = stories;
    }

    @Override
    public String execute(SuggestBranchNameQuery query) {
        UserStory story = stories.asMember(query.userId(), query.workspaceId(), query.key());

        return BranchNames.suggest(story.getKey(), story.getTitle());
    }
}
