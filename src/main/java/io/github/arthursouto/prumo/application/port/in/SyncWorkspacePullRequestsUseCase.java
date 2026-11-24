package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.SyncWorkspacePullRequestsCommand;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import java.util.List;

public interface SyncWorkspacePullRequestsUseCase {

    List<ConnectedRepository> execute(SyncWorkspacePullRequestsCommand command);
}
