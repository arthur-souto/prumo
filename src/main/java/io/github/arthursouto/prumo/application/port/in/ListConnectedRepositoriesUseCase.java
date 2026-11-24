package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListConnectedRepositoriesQuery;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import java.util.List;

public interface ListConnectedRepositoriesUseCase {

    List<ConnectedRepository> execute(ListConnectedRepositoriesQuery command);
}
