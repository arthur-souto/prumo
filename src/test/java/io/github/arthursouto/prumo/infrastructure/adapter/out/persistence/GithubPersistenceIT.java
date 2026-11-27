package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.ConnectedRepositoryPersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.GithubConnectionPersistenceAdapter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The GitHub token and the connected repositories against the real schema: the mapping of every column, the
 * case-insensitive lookup used to avoid connecting a repository twice and the unique constraint behind it.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GithubConnectionPersistenceAdapter.class, ConnectedRepositoryPersistenceAdapter.class})
class GithubPersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    private GithubConnectionPersistenceAdapter connections;

    @Autowired
    private ConnectedRepositoryPersistenceAdapter repositories;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private UUID workspaceId;

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'U', ?, 'USER', true, now(), now())",
                id, id + "@test.dev");
        return id;
    }

    private UUID workspace(UUID ownerId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO workspaces (id, name, slug, owner_id, created_at, updated_at) VALUES (?, 'W', ?, ?, now(), now())",
                id, "w-" + id, ownerId);
        return id;
    }

    private ConnectedRepository repository(UUID workspace, String owner, String name, Instant createdAt) {
        return ConnectedRepository.restore(UUID.randomUUID(), workspace, owner, name, "main",
                "https://github.com/" + owner + "/" + name, userId, createdAt, null, null, null);
    }

    /** Sends the pending writes and drops the cache, so the next read really comes from PostgreSQL. */
    private void reload() {
        entityManager.flush();
        entityManager.clear();
    }

    @BeforeEach
    void setUp() {
        userId = user();
        workspaceId = workspace(userId);
    }

    @Test
    void theConnectionIsStoredPerUserAndReplacedWhenSavedAgain() {
        connections.save(new GithubConnection(userId, "cipher-1", "arthur", T0));
        connections.save(new GithubConnection(userId, "cipher-2", "arthur-souto", T0.plusSeconds(60)));
        reload();

        assertThat(connections.findByUserId(userId)).contains(
                new GithubConnection(userId, "cipher-2", "arthur-souto", T0.plusSeconds(60))
        );
        assertThat(connections.findByUserId(user())).isEmpty();
    }

    @Test
    void deletingTheConnectionLeavesTheOtherUsersAlone() {
        UUID other = user();
        connections.save(new GithubConnection(userId, "cipher", "a", T0));
        connections.save(new GithubConnection(other, "cipher", "b", T0));
        reload();

        connections.deleteByUserId(userId);
        reload();

        assertThat(connections.findByUserId(userId)).isEmpty();
        assertThat(connections.findByUserId(other)).isPresent();
    }

    @Test
    void aRepositoryKeepsEveryFieldIncludingTheSyncState() {
        ConnectedRepository repository = repository(workspaceId, "arthur", "prumo", T0);
        repository.markSynced(T0.plusSeconds(30), "W/\"etag\"");
        repositories.save(repository);
        reload();

        ConnectedRepository loaded = repositories.findById(repository.getId()).orElseThrow();

        assertThat(loaded).usingRecursiveComparison().isEqualTo(repository);
    }

    @Test
    void aFailedSyncKeepsTheErrorCodeAndTheLastEtag() {
        ConnectedRepository repository = repository(workspaceId, "arthur", "prumo", T0);
        repository.markSynced(T0, "\"v1\"");
        repositories.save(repository);
        repository.markFailed(T0.plusSeconds(60), "RATE_LIMITED");
        repositories.save(repository);
        reload();

        ConnectedRepository loaded = repositories.findById(repository.getId()).orElseThrow();

        assertThat(loaded.getLastSyncError()).isEqualTo("RATE_LIMITED");
        assertThat(loaded.getLastSyncedAt()).isEqualTo(T0.plusSeconds(60));
        assertThat(loaded.getEtag()).isEqualTo("\"v1\"");
    }

    @Test
    void theRepositoriesOfAWorkspaceComeInTheOrderTheyWereConnected() {
        UUID otherWorkspace = workspace(userId);
        ConnectedRepository second = repositories.save(repository(workspaceId, "arthur", "b", T0.plusSeconds(10)));
        ConnectedRepository first = repositories.save(repository(workspaceId, "arthur", "a", T0));
        ConnectedRepository foreign = repositories.save(repository(otherWorkspace, "arthur", "c", T0));
        reload();

        assertThat(repositories.findAllByWorkspaceId(workspaceId)).extracting(ConnectedRepository::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(repositories.findAll()).extracting(ConnectedRepository::getId)
                .contains(first.getId(), second.getId(), foreign.getId());
    }

    @Test
    void theLookupByOwnerAndNameIgnoresCaseAndStaysInsideTheWorkspace() {
        ConnectedRepository saved = repositories.save(repository(workspaceId, "arthur", "prumo", T0));
        reload();

        assertThat(repositories.findByWorkspaceIdAndOwnerAndName(workspaceId, "ARTHUR", "Prumo"))
                .map(ConnectedRepository::getId).contains(saved.getId());
        assertThat(repositories.findByWorkspaceIdAndOwnerAndName(workspace(userId), "arthur", "prumo")).isEmpty();
        assertThat(repositories.findByWorkspaceIdAndOwnerAndName(workspaceId, "arthur", "other")).isEmpty();
    }

    @Test
    void theSameRepositoryCannotBeConnectedTwiceToAWorkspace() {
        repositories.save(repository(workspaceId, "arthur", "prumo", T0));
        reload();

        assertThatThrownBy(() -> {
            repositories.save(repository(workspaceId, "arthur", "prumo", T0));
            entityManager.flush();
        }).isInstanceOf(RuntimeException.class).hasMessageContaining("uq_connected_repositories");
    }

    @Test
    void aDisconnectedRepositoryIsGone() {
        ConnectedRepository saved = repositories.save(repository(workspaceId, "arthur", "prumo", T0));
        reload();

        repositories.deleteById(saved.getId());
        reload();

        assertThat(repositories.findById(saved.getId())).isEmpty();
    }
}
