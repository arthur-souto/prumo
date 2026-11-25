package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetSprintQuery;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;

public interface GetSprintUseCase {

    SprintView execute(GetSprintQuery query);
}
