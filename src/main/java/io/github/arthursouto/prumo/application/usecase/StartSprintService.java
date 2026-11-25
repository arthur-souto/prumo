package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.StartSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.command.SprintCommand;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

import java.util.List;

public class StartSprintService implements StartSprintUseCase {

    private final SprintLookup sprints;
    private final SprintRepositoryPort sprintRepository;
    private final StoryRepositoryPort storyRepository;
    private final SprintViews views;

    public StartSprintService(SprintLookup sprints,
                              SprintRepositoryPort sprintRepository,
                              StoryRepositoryPort storyRepository,
                              SprintViews views) {
        this.sprints = sprints;
        this.sprintRepository = sprintRepository;
        this.storyRepository = storyRepository;
        this.views = views;
    }

    @Override
    public SprintView execute(SprintCommand command) {
        Sprint sprint = sprints.asEditor(command.userId(), command.workspaceId(), command.sprintId());

        if (!sprint.isPlanned()) {
            throw new BusinessRuleException("Only a planned sprint can be started");
        }

        if (sprintRepository.findActive(command.workspaceId()).isPresent()) {
            throw new ConflictException("Another sprint is already active");
        }

        sprint.start(SprintViews.points(storyRepository.findAllBySprintIds(List.of(sprint.getId()))));

        return views.of(sprintRepository.save(sprint));
    }
}
