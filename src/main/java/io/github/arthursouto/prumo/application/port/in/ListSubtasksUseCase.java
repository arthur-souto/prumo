package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListSubtasksQuery;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;

import java.util.List;

public interface ListSubtasksUseCase {

    List<SubtaskView> execute(ListSubtasksQuery query);
}
