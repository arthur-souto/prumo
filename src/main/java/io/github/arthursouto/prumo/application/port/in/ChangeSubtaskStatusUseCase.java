package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ChangeSubtaskStatusCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;

public interface ChangeSubtaskStatusUseCase {

    SubtaskView execute(ChangeSubtaskStatusCommand command);
}
