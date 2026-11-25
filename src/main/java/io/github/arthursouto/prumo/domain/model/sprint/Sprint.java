package io.github.arthursouto.prumo.domain.model.sprint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class Sprint {

    private final UUID id;
    private final UUID workspaceId;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private SprintState state;
    private Integer committedPoints;
    private Integer completedPoints;
    private Instant startedAt;
    private Instant closedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private Sprint(UUID id,
                   UUID workspaceId,
                   String name,
                   String goal,
                   LocalDate startDate,
                   LocalDate endDate,
                   SprintState state,
                   Integer committedPoints,
                   Integer completedPoints,
                   Instant startedAt,
                   Instant closedAt,
                   Instant createdAt,
                   Instant updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.name = name;
        this.goal = goal;
        this.startDate = startDate;
        this.endDate = endDate;
        this.state = state;
        this.committedPoints = committedPoints;
        this.completedPoints = completedPoints;
        this.startedAt = startedAt;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Sprint create(UUID workspaceId, String name, String goal, LocalDate startDate, LocalDate endDate) {
        Instant now = Instant.now();
        return new Sprint(UUID.randomUUID(), workspaceId, name, goal, startDate, endDate, SprintState.PLANNED,
                null, null, null, null, now, now);
    }

    public static Sprint restore(UUID id,
                                 UUID workspaceId,
                                 String name,
                                 String goal,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 SprintState state,
                                 Integer committedPoints,
                                 Integer completedPoints,
                                 Instant startedAt,
                                 Instant closedAt,
                                 Instant createdAt,
                                 Instant updatedAt) {
        return new Sprint(id, workspaceId, name, goal, startDate, endDate, state, committedPoints, completedPoints,
                startedAt, closedAt, createdAt, updatedAt);
    }

    public void update(String name, String goal, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.goal = goal;
        this.startDate = startDate;
        this.endDate = endDate;
        touch();
    }

    public void start(int committedPoints) {
        this.state = SprintState.ACTIVE;
        this.committedPoints = committedPoints;
        this.startedAt = Instant.now();
        touch();
    }

    public void close(int completedPoints) {
        this.state = SprintState.CLOSED;
        this.completedPoints = completedPoints;
        this.closedAt = Instant.now();
        touch();
    }

    public boolean isPlanned() {
        return state == SprintState.PLANNED;
    }

    public boolean isActive() {
        return state == SprintState.ACTIVE;
    }

    public boolean isClosed() {
        return state == SprintState.CLOSED;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public String getGoal() {
        return goal;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public SprintState getState() {
        return state;
    }

    public Integer getCommittedPoints() {
        return committedPoints;
    }

    public Integer getCompletedPoints() {
        return completedPoints;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
