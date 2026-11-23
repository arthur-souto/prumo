package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListWorkspacesQuery;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

import java.util.List;

public interface ListWorkspacesUseCase {

    List<Workspace> execute(ListWorkspacesQuery query);
}
