package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.DeleteSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.command.SprintCommand;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

public class DeleteSprintService implements DeleteSprintUseCase {

    private final SprintLookup sprints;
    private final SprintRepositoryPort sprintRepository;

    public DeleteSprintService(SprintLookup sprints, SprintRepositoryPort sprintRepository) {
        this.sprints = sprints;
        this.sprintRepository = sprintRepository;
    }

    @Override
    public void execute(SprintCommand command) {
        Sprint sprint = sprints.asEditor(command.userId(), command.workspaceId(), command.sprintId());

        if (!sprint.isPlanned()) {
            throw new BusinessRuleException("Only a planned sprint can be deleted");
        }

        sprintRepository.delete(sprint);
    }
}
