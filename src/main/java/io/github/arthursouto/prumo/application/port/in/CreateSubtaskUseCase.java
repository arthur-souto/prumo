package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;

public interface CreateSubtaskUseCase {

    SubtaskView execute(CreateSubtaskCommand command);
}
