package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.UpdateStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.UpdateStoryCommand;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class UpdateStoryService implements UpdateStoryUseCase {

    private final StoryLookup stories;
    private final StoryRepositoryPort storyRepository;

    public UpdateStoryService(StoryLookup stories, StoryRepositoryPort storyRepository) {
        this.stories = stories;
        this.storyRepository = storyRepository;
    }

    @Override
    public UserStory execute(UpdateStoryCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.key());

        story.update(
                command.title().strip(),
                Checklists.text(command.description()),
                command.priority() == null ? StoryPriority.MEDIUM : command.priority(),
                command.storyPoints(),
                Checklists.labels(command.labels()),
                Checklists.items(command.acceptanceCriteria(), story.getAcceptanceCriteria())
        );

        return storyRepository.save(story);
    }
}
