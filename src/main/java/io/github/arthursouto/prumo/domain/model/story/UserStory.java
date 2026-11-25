package io.github.arthursouto.prumo.domain.model.story;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class UserStory {

    private final UUID id;
    private final UUID workspaceId;
    private final int number;
    private final String key;
    private String title;
    private String description;
    private StoryPriority priority;
    private Integer storyPoints;
    private UUID columnId;
    private ChangeSource columnSource = ChangeSource.USER;
    private String rank;
    private List<String> labels;
    private List<ChecklistItem> acceptanceCriteria;
    private UUID repositoryId;
    private UUID assigneeId;
    private UUID sprintId;
    private final List<PullRequest> pullRequests;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserStory(UUID id,
                      UUID workspaceId,
                      int number,
                      String key,
                      String title,
                      String description,
                      StoryPriority priority,
                      Integer storyPoints,
                      UUID columnId,
                      String rank,
                      List<String> labels,
                      List<ChecklistItem> acceptanceCriteria,
                      UUID repositoryId,
                      List<PullRequest> pullRequests,
                      Instant createdAt,
                      Instant updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.number = number;
        this.key = key;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.storyPoints = storyPoints;
        this.columnId = columnId;
        this.rank = rank;
        this.labels = List.copyOf(labels);
        this.acceptanceCriteria = List.copyOf(acceptanceCriteria);
        this.repositoryId = repositoryId;
        this.pullRequests = List.copyOf(pullRequests);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserStory create(UUID workspaceId,
                                   int number,
                                   String key,
                                   String title,
                                   String description,
                                   StoryPriority priority,
                                   Integer storyPoints,
                                   UUID columnId,
                                   String rank,
                                   List<String> labels,
                                                List<ChecklistItem> acceptanceCriteria) {
        Instant now = Instant.now();

        return new UserStory(
                UUID.randomUUID(),
                workspaceId,
                number,
                key,
                title,
                description,
                priority,
                storyPoints,
                columnId,
                rank,
                labels,
                acceptanceCriteria,
                null,
                List.of(),
                now,
                now
        );
    }

    public static UserStory restore(UUID id,
                                    UUID workspaceId,
                                    int number,
                                    String key,
                                    String title,
                                    String description,
                                    StoryPriority priority,
                                    Integer storyPoints,
                                    UUID columnId,
                                    String rank,
                                    List<String> labels,
                                                  List<ChecklistItem> acceptanceCriteria,
                                    UUID repositoryId,
                                                  List<PullRequest> pullRequests,
                                    Instant createdAt,
                                    Instant updatedAt) {
        return new UserStory(
                id,
                workspaceId,
                number,
                key,
                title,
                description,
                priority,
                storyPoints,
                columnId,
                rank,
                labels,
                acceptanceCriteria,
                repositoryId,
                pullRequests,
                createdAt,
                updatedAt
        );
    }

    public void update(String title,
                       String description,
                       StoryPriority priority,
                       Integer storyPoints,
                       List<String> labels,
                        List<ChecklistItem> acceptanceCriteria) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.storyPoints = storyPoints;
        this.labels = List.copyOf(labels);
        this.acceptanceCriteria = List.copyOf(acceptanceCriteria);
        touch();
    }

    public void moveTo(UUID columnId, String rank) {
        moveTo(columnId, rank, ChangeSource.USER);
    }

    public void moveTo(UUID columnId, String rank, ChangeSource source) {
        this.columnId = columnId;
        this.rank = rank;
        this.columnSource = source;
        touch();
    }

    /** Used when rebuilding the story from storage, without touching updatedAt. */
    public UserStory withColumnSource(ChangeSource columnSource) {
        this.columnSource = columnSource;
        return this;
    }

    public ChangeSource getColumnSource() {
        return columnSource;
    }

    public boolean setChecklistItemDone(UUID itemId, boolean done) {
        List<ChecklistItem> updated = replace(acceptanceCriteria, itemId, done);

        if (updated == null) {
            return false;
        }

        this.acceptanceCriteria = updated;
        touch();
        return true;
    }

    public void linkRepository(UUID repositoryId) {
        this.repositoryId = repositoryId;
        touch();
    }

    public void unlinkRepository() {
        this.repositoryId = null;
        touch();
    }

    private static List<ChecklistItem> replace(List<ChecklistItem> items, UUID itemId, boolean done) {
        if (items.stream().noneMatch(item -> item.id().equals(itemId))) {
            return null;
        }

        List<ChecklistItem> result = new ArrayList<>();
        items.forEach(item -> result.add(item.id().equals(itemId) ? item.withDone(done) : item));
        return List.copyOf(result);
    }

    public void assignTo(UUID assigneeId) {
        this.assigneeId = assigneeId;
        touch();
    }

    /** Used when rebuilding the story from storage, without touching updatedAt. */
    public UserStory withAssignee(UUID assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void planInto(UUID sprintId) {
        this.sprintId = sprintId;
        touch();
    }

    public UserStory withSprint(UUID sprintId) {
        this.sprintId = sprintId;
        return this;
    }

    public UUID getSprintId() {
        return sprintId;
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

    public StoryPriority getPriority() {
        return priority;
    }

    public Integer getStoryPoints() {
        return storyPoints;
    }

    public UUID getColumnId() {
        return columnId;
    }

    public String getRank() {
        return rank;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<ChecklistItem> getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public List<PullRequest> getPullRequests() {
        return pullRequests;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
