package io.github.arthursouto.prumo.domain.model.story;

import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/** A piece of work inside a story. It has its own key, status and, at most, one branch. */
public final class Subtask {

    private final UUID id;
    private final UUID storyId;
    private final UUID workspaceId;
    private final int number;
    private final String key;
    private String title;
    private String description;
    private SubtaskStatus status;
    private ChangeSource statusSource = ChangeSource.USER;
    private int position;
    private String branchName;
    private final Instant createdAt;
    private Instant updatedAt;

    private Subtask(UUID id,
                    UUID storyId,
                    UUID workspaceId,
                    int number,
                    String key,
                    String title,
                    String description,
                    SubtaskStatus status,
                    int position,
                    String branchName,
                    Instant createdAt,
                    Instant updatedAt) {
        this.id = id;
        this.storyId = storyId;
        this.workspaceId = workspaceId;
        this.number = number;
        this.key = key;
        this.title = title;
        this.description = description;
        this.status = status;
        this.position = position;
        this.branchName = branchName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Subtask create(UUID storyId,
                                 UUID workspaceId,
                                 int number,
                                 String key,
                                 String title,
                                 String description,
                                 int position) {
        Instant now = Instant.now();

        return new Subtask(UUID.randomUUID(), storyId, workspaceId, number, key, title, description,
                SubtaskStatus.TO_DO, position, null, now, now);
    }

    public static Subtask restore(UUID id,
                                  UUID storyId,
                                  UUID workspaceId,
                                  int number,
                                  String key,
                                  String title,
                                  String description,
                                  SubtaskStatus status,
                                  int position,
                                  String branchName,
                                  Instant createdAt,
                                  Instant updatedAt) {
        return new Subtask(id, storyId, workspaceId, number, key, title, description, status, position,
                branchName, createdAt, updatedAt);
    }

    /** True when there is at least one subtask and every one of them is done. */
    public static boolean allDone(Collection<Subtask> subtasks) {
        return !subtasks.isEmpty() && subtasks.stream().allMatch(subtask -> subtask.status == SubtaskStatus.DONE);
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
        touch();
    }

    /** Returns true when the status actually changed. */
    public boolean changeStatus(SubtaskStatus status) {
        return changeStatus(status, ChangeSource.USER);
    }

    /** Returns true when the status actually changed; the source only moves along with a real change. */
    public boolean changeStatus(SubtaskStatus status, ChangeSource source) {
        if (this.status == status) {
            return false;
        }

        this.status = status;
        this.statusSource = source;
        touch();
        return true;
    }

    /** Used when rebuilding the subtask from storage, without touching updatedAt. */
    public Subtask withStatusSource(ChangeSource statusSource) {
        this.statusSource = statusSource;
        return this;
    }

    public void moveTo(int position) {
        this.position = position;
        touch();
    }

    public void recordBranch(String branchName) {
        if (this.branchName != null) {
            throw new BusinessRuleException("A subtask can have only one branch");
        }

        this.branchName = branchName;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStoryId() {
        return storyId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public int getNumber() {
        return number;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public SubtaskStatus getStatus() {
        return status;
    }

    public ChangeSource getStatusSource() {
        return statusSource;
    }

    public int getPosition() {
        return position;
    }

    public String getBranchName() {
        return branchName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
