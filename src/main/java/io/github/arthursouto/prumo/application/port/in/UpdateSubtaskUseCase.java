package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.UpdateSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;

public interface UpdateSubtaskUseCase {

    SubtaskView execute(UpdateSubtaskCommand command);
}
