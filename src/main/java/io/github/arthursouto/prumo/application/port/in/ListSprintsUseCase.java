package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListSprintsQuery;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;

public interface ListSprintsUseCase {

    PageResult<SprintView> execute(ListSprintsQuery query);
}
