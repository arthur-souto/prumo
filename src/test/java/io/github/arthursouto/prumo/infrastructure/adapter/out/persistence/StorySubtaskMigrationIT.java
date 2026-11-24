package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migrates a database that already holds stories, checklist subtasks, branches and pull requests
 * (schema V10) to V11 and checks that nothing is lost or orphaned.
 */
class StorySubtaskMigrationIT {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final AtomicInteger DATABASES = new AtomicInteger();

    @BeforeAll
    static void start() {
        POSTGRES.start();
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    private static JdbcTemplate newDatabaseAtV10() {
        String name = "mig" + DATABASES.incrementAndGet();
        new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()))
                .execute("CREATE DATABASE " + name);
        String url = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(\\?|$)", "/" + name + "$1");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
        flyway(dataSource, "10").migrate();

        return new JdbcTemplate(dataSource);
    }

    private static Flyway flyway(DriverManagerDataSource dataSource, String target) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target(target).load();
    }

    private static void migrateToLatest(JdbcTemplate jdbc) {
        flyway((DriverManagerDataSource) jdbc.getDataSource(), "latest").migrate();
    }

    private record Seed(UUID workspaceId, UUID repositoryId, UUID todo, UUID plain, UUID merged, UUID reviewing) {
    }

    /**
     * Board MIG with stories MIG-1..MIG-3:
     * MIG-1 has two checklist subtasks (one done) and a criterion, no branch;
     * MIG-2 has a branch with a merged pull request, plus one that only cites the story;
     * MIG-3 has a checklist subtask, a branch and an open pull request.
     */
    private static Seed seed(JdbcTemplate jdbc) {
        UUID user = UUID.randomUUID();
        UUID workspace = UUID.randomUUID();
        UUID repository = UUID.randomUUID();
        UUID todo = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, name, email, role, enabled, created_at, updated_at) VALUES (?, 'U', 'u@test.dev', 'USER', true, now(), now())", user);
        jdbc.update("INSERT INTO workspaces (id, name, slug, owner_id, created_at, updated_at) VALUES (?, 'W', 'w', ?, now(), now())", workspace, user);
        jdbc.update("INSERT INTO boards (workspace_id, key_prefix, next_story_number) VALUES (?, 'MIG', 4)", workspace);
        jdbc.update("INSERT INTO board_columns (id, workspace_id, name, kind, position, created_at) VALUES (?, ?, 'To Do', 'TODO', 0, now())", todo, workspace);
        jdbc.update("INSERT INTO connected_repositories (id, workspace_id, owner, name, default_branch, html_url, connected_by, created_at) VALUES (?, ?, 'o', 'r', 'main', 'https://github.com/o/r', ?, now())", repository, workspace, user);

        UUID plain = story(jdbc, workspace, todo, 1, null, "2026-01-01T10:00:00Z");
        UUID merged = story(jdbc, workspace, todo, 2, "feature/MIG-2-login", "2026-01-02T10:00:00Z");
        UUID reviewing = story(jdbc, workspace, todo, 3, "feature/MIG-3-export", "2026-01-03T10:00:00Z");

        item(jdbc, plain, "SUBTASK", "write code", true, 0);
        item(jdbc, plain, "SUBTASK", "write tests", false, 1);
        item(jdbc, plain, "CRITERION", "it works", false, 0);
        item(jdbc, reviewing, "SUBTASK", "design", false, 0);

        pullRequest(jdbc, merged, repository, 10, "MERGED", "feature/MIG-2-login");
        pullRequest(jdbc, merged, repository, 11, "OPEN", "someone/else");
        pullRequest(jdbc, reviewing, repository, 12, "OPEN", "feature/MIG-3-export");

        return new Seed(workspace, repository, todo, plain, merged, reviewing);
    }

    private static UUID story(JdbcTemplate jdbc, UUID workspace, UUID column, int number, String branch, String createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO user_stories (id, workspace_id, story_number, story_key, title, priority, column_id, sort_rank, branch_name, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, 'Story', 'MEDIUM', ?, ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz))",
                id, workspace, number, "MIG-" + number, column, "V" + number, branch, createdAt, createdAt);

        return id;
    }

    private static void item(JdbcTemplate jdbc, UUID story, String kind, String text, boolean done, int position) {
        jdbc.update("INSERT INTO story_checklist_items (id, story_id, kind, text, done, position) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), story, kind, text, done, position);
    }

    private static void pullRequest(JdbcTemplate jdbc, UUID story, UUID repository, int number, String state, String branch) {
        jdbc.update("INSERT INTO story_pull_requests (id, story_id, repository_id, number, title, state, url, branch, updated_at) "
                + "VALUES (?, ?, ?, ?, 'PR', ?, 'https://github.com/o/r/pull/' || ?, ?, now())",
                UUID.randomUUID(), story, repository, number, state, number, branch);
    }

    @Test
    void checklistSubtasksBecomeSubtasksWithMappedStatusAndKeysThatContinueTheSequence() {
        JdbcTemplate jdbc = newDatabaseAtV10();
        Seed seed = seed(jdbc);

        migrateToLatest(jdbc);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT subtask_key, title, status, position, story_id FROM story_subtasks ORDER BY subtask_number");
        // MIG-1: 2 checklist items; MIG-2: initial; MIG-3: 1 checklist item + initial  => 5 subtasks, keys MIG-4..MIG-8
        assertThat(rows).extracting(row -> row.get("subtask_key"))
                .containsExactly("MIG-4", "MIG-5", "MIG-6", "MIG-7", "MIG-8");
        assertThat(rows.get(0)).containsEntry("title", "write code").containsEntry("status", "DONE").containsEntry("position", 0)
                .containsEntry("story_id", seed.plain());
        assertThat(rows.get(1)).containsEntry("title", "write tests").containsEntry("status", "TO_DO").containsEntry("position", 1);
        assertThat(jdbc.queryForObject("SELECT next_story_number FROM boards WHERE workspace_id = ?", Integer.class, seed.workspaceId()))
                .isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT subtask_key) FROM story_subtasks", Integer.class)).isEqualTo(5);
    }

    @Test
    void storiesWithABranchGetAnInitialSubtaskThatKeepsTheBranchAndItsPullRequests() {
        JdbcTemplate jdbc = newDatabaseAtV10();
        Seed seed = seed(jdbc);

        migrateToLatest(jdbc);

        Map<String, Object> mergedInitial = jdbc.queryForMap(
                "SELECT * FROM story_subtasks WHERE story_id = ? AND branch_name IS NOT NULL", seed.merged());
        assertThat(mergedInitial).containsEntry("title", "Implementação inicial")
                .containsEntry("branch_name", "feature/MIG-2-login").containsEntry("status", "DONE");
        Map<String, Object> reviewingInitial = jdbc.queryForMap(
                "SELECT * FROM story_subtasks WHERE story_id = ? AND branch_name IS NOT NULL", seed.reviewing());
        assertThat(reviewingInitial).containsEntry("branch_name", "feature/MIG-3-export").containsEntry("status", "IN_REVIEW")
                // placed after the story's existing checklist subtask
                .containsEntry("position", 1);

        // the pull request opened from the branch follows it; the one that only cites the story stays on the story
        assertThat(jdbc.queryForObject("SELECT subtask_id FROM story_pull_requests WHERE number = 10", UUID.class))
                .isEqualTo(mergedInitial.get("id"));
        assertThat(jdbc.queryForObject("SELECT subtask_id FROM story_pull_requests WHERE number = 12", UUID.class))
                .isEqualTo(reviewingInitial.get("id"));
        assertThat(jdbc.queryForObject("SELECT subtask_id FROM story_pull_requests WHERE number = 11", UUID.class)).isNull();
    }

    @Test
    void nothingIsLostOrLeftOrphaned() {
        JdbcTemplate jdbc = newDatabaseAtV10();
        seed(jdbc);
        int pullRequestsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM story_pull_requests", Integer.class);
        int branchesBefore = jdbc.queryForObject("SELECT COUNT(*) FROM user_stories WHERE branch_name IS NOT NULL", Integer.class);

        migrateToLatest(jdbc);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM story_pull_requests", Integer.class)).isEqualTo(pullRequestsBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM story_pull_requests WHERE story_id IS NULL", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM story_subtasks WHERE branch_name IS NOT NULL", Integer.class))
                .isEqualTo(branchesBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM story_checklist_items WHERE kind = 'SUBTASK'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM story_checklist_items WHERE kind = 'CRITERION'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'user_stories' AND column_name = 'branch_name'",
                Integer.class)).isZero();
    }

    @Test
    void aDatabaseWithoutStoriesMigratesCleanly() {
        JdbcTemplate jdbc = newDatabaseAtV10();

        migrateToLatest(jdbc);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM story_subtasks", Integer.class)).isZero();
    }

    @Test
    void theManualUndoScriptRestoresChecklistsAndBranches() throws Exception {
        JdbcTemplate jdbc = newDatabaseAtV10();
        Seed seed = seed(jdbc);
        migrateToLatest(jdbc);

        jdbc.execute(Files.readString(Path.of("src/main/resources/db/undo/U11__create_story_subtasks.sql")));

        assertThat(jdbc.queryForObject("SELECT branch_name FROM user_stories WHERE id = ?", String.class, seed.merged()))
                .isEqualTo("feature/MIG-2-login");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM story_checklist_items WHERE kind = 'SUBTASK' AND story_id = ?", Integer.class, seed.plain()))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM story_checklist_items WHERE kind = 'SUBTASK' AND story_id = ? AND done", Integer.class, seed.plain()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM story_pull_requests", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT to_regclass('story_subtasks')", String.class)).isNull();
    }
}
