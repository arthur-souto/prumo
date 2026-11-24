package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListAccessibleRepositoriesQuery;
import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;
import java.util.List;

public interface ListAccessibleRepositoriesUseCase {

    List<RemoteRepository> execute(ListAccessibleRepositoriesQuery command);
}
