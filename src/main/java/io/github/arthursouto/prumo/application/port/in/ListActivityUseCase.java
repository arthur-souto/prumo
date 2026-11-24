package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListActivityQuery;
import io.github.arthursouto.prumo.application.port.in.result.ActivityView;

import java.util.List;

public interface ListActivityUseCase {

    List<ActivityView> execute(ListActivityQuery query);
}
