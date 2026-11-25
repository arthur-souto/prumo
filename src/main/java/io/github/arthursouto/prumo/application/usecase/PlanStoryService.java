package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.PlanStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.PlanStoryCommand;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public class PlanStoryService implements PlanStoryUseCase {

    private final StoryLookup stories;
    private final SprintLookup sprints;
    private final StoryRepositoryPort storyRepository;

    public PlanStoryService(StoryLookup stories, SprintLookup sprints, StoryRepositoryPort storyRepository) {
        this.stories = stories;
        this.sprints = sprints;
        this.storyRepository = storyRepository;
    }

    @Override
    public UserStory execute(PlanStoryCommand command) {
        UserStory story = stories.asMember(command.userId(), command.workspaceId(), command.key());

        if (command.sprintId() != null) {
            Sprint sprint = sprints.find(command.workspaceId(), command.sprintId());

            if (sprint.isClosed()) {
                throw new BusinessRuleException("A story cannot be planned into a closed sprint");
            }
        }

        story.planInto(command.sprintId());

        return storyRepository.save(story);
    }
}
