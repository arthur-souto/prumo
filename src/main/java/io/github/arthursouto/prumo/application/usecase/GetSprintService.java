package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetSprintQuery;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;

public class GetSprintService implements GetSprintUseCase {

    private final SprintLookup sprints;
    private final SprintViews views;

    public GetSprintService(SprintLookup sprints, SprintViews views) {
        this.sprints = sprints;
        this.views = views;
    }

    @Override
    public SprintView execute(GetSprintQuery query) {
        return views.of(sprints.asMember(query.userId(), query.workspaceId(), query.sprintId()));
    }
}
