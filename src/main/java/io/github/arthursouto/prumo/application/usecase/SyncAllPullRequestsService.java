package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.SyncAllPullRequestsUseCase;
import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;

public class SyncAllPullRequestsService implements SyncAllPullRequestsUseCase {

    private final ConnectedRepositoryPort repositoryPort;
    private final PullRequestSyncer syncer;

    public SyncAllPullRequestsService(ConnectedRepositoryPort repositoryPort, PullRequestSyncer syncer) {
        this.repositoryPort = repositoryPort;
        this.syncer = syncer;
    }

    @Override
    public void execute() {
        repositoryPort.findAll().forEach(repository -> {
            try {
                syncer.sync(repository);
            } catch (RuntimeException exception) {
                repository.markFailed(java.time.Instant.now(), "UNEXPECTED");
                repositoryPort.save(repository);
            }
        });
    }
}
