package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetSubtaskQuery;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;

public interface GetSubtaskUseCase {

    SubtaskView execute(GetSubtaskQuery query);
}
