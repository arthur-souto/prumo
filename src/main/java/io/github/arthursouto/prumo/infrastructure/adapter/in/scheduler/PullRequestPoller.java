package io.github.arthursouto.prumo.infrastructure.adapter.in.scheduler;

import io.github.arthursouto.prumo.application.port.in.SyncAllPullRequestsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.github.polling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PullRequestPoller {

    private final SyncAllPullRequestsUseCase syncAll;

    public PullRequestPoller(SyncAllPullRequestsUseCase syncAll) {
        this.syncAll = syncAll;
    }

    @Scheduled(
            fixedDelayString = "${app.github.polling.interval:60s}",
            initialDelayString = "${app.github.polling.interval:60s}"
    )
    public void poll() {
        syncAll.execute();
    }
}
