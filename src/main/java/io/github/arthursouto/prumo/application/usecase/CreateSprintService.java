package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.CreateSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateSprintCommand;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

public class CreateSprintService implements CreateSprintUseCase {

    private final WorkspaceAccess access;
    private final SprintRepositoryPort sprintRepository;
    private final SprintViews views;

    public CreateSprintService(WorkspaceAccess access, SprintRepositoryPort sprintRepository, SprintViews views) {
        this.access = access;
        this.sprintRepository = sprintRepository;
        this.views = views;
    }

    @Override
    public SprintView execute(CreateSprintCommand command) {
        access.editor(command.userId(), command.workspaceId());

        Sprint sprint = Sprint.create(
                command.workspaceId(),
                command.name().strip(),
                SprintText.goal(command.goal()),
                command.startDate(),
                command.endDate()
        );

        return views.of(sprintRepository.save(sprint));
    }
}
