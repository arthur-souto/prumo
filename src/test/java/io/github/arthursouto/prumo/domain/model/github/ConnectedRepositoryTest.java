package io.github.arthursouto.prumo.domain.model.github;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectedRepositoryTest {

    private final ConnectedRepository repository = ConnectedRepository.connect(
            UUID.randomUUID(),
            "arthur",
            "prumo",
            "main",
            "https://github.com/arthur/prumo",
            UUID.randomUUID()
    );

    @Test
    void startsNeverSynced() {
        assertThat(repository.getLastSyncedAt()).isNull();
        assertThat(repository.getLastSyncError()).isNull();
        assertThat(repository.getEtag()).isNull();
        assertThat(repository.fullName()).isEqualTo("arthur/prumo");
    }

    @Test
    void aSuccessfulSyncStoresTheEtagAndClearsTheError() {
        Instant now = Instant.now();
        repository.markFailed(now, "RATE_LIMITED");

        repository.markSynced(now, "\"abc\"");

        assertThat(repository.getEtag()).isEqualTo("\"abc\"");
        assertThat(repository.getLastSyncError()).isNull();
        assertThat(repository.getLastSyncedAt()).isEqualTo(now);
    }

    @Test
    void aFailedSyncKeepsTheEtagAndRecordsTheReason() {
        repository.markSynced(Instant.now(), "\"abc\"");

        repository.markFailed(Instant.now(), "INVALID_TOKEN");

        assertThat(repository.getEtag()).isEqualTo("\"abc\"");
        assertThat(repository.getLastSyncError()).isEqualTo("INVALID_TOKEN");
    }
}
