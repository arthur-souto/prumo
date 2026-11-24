package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetSubtaskQuery;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;

public class GetSubtaskService implements GetSubtaskUseCase {

    private final SubtaskLookup lookup;

    public GetSubtaskService(SubtaskLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public SubtaskView execute(GetSubtaskQuery query) {
        return lookup.view(lookup.asMember(query.userId(), query.workspaceId(), query.key()));
    }
}
