package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.UpdateSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.command.UpdateSprintCommand;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

public class UpdateSprintService implements UpdateSprintUseCase {

    private final SprintLookup sprints;
    private final SprintRepositoryPort sprintRepository;
    private final SprintViews views;

    public UpdateSprintService(SprintLookup sprints, SprintRepositoryPort sprintRepository, SprintViews views) {
        this.sprints = sprints;
        this.sprintRepository = sprintRepository;
        this.views = views;
    }

    @Override
    public SprintView execute(UpdateSprintCommand command) {
        Sprint sprint = sprints.asEditor(command.userId(), command.workspaceId(), command.sprintId());

        if (sprint.isClosed()) {
            throw new BusinessRuleException("A closed sprint cannot be changed");
        }

        sprint.update(command.name().strip(), SprintText.goal(command.goal()), command.startDate(), command.endDate());

        return views.of(sprintRepository.save(sprint));
    }
}
