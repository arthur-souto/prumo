package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskBranchCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;

public interface CreateSubtaskBranchUseCase {

    SubtaskView execute(CreateSubtaskBranchCommand command);
}
