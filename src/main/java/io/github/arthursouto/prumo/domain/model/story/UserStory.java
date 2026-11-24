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
    private String rank;
    private List<String> labels;
    private List<ChecklistItem> subtasks;
    private List<ChecklistItem> acceptanceCriteria;
    private UUID repositoryId;
    private String branchName;
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
                      List<ChecklistItem> subtasks,
                      List<ChecklistItem> acceptanceCriteria,
                      UUID repositoryId,
                      String branchName,
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
        this.subtasks = List.copyOf(subtasks);
        this.acceptanceCriteria = List.copyOf(acceptanceCriteria);
        this.repositoryId = repositoryId;
        this.branchName = branchName;
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
                                   List<ChecklistItem> subtasks,
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
                subtasks,
                acceptanceCriteria,
                null,
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
                                    List<ChecklistItem> subtasks,
                                    List<ChecklistItem> acceptanceCriteria,
                                    UUID repositoryId,
                                    String branchName,
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
                subtasks,
                acceptanceCriteria,
                repositoryId,
                branchName,
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
                       List<ChecklistItem> subtasks,
                       List<ChecklistItem> acceptanceCriteria) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.storyPoints = storyPoints;
        this.labels = List.copyOf(labels);
        this.subtasks = List.copyOf(subtasks);
        this.acceptanceCriteria = List.copyOf(acceptanceCriteria);
        touch();
    }

    public void moveTo(UUID columnId, String rank) {
        this.columnId = columnId;
        this.rank = rank;
        touch();
    }

    public boolean setChecklistItemDone(UUID itemId, boolean done) {
        List<ChecklistItem> updatedSubtasks = replace(subtasks, itemId, done);

        if (updatedSubtasks != null) {
            this.subtasks = updatedSubtasks;
            touch();
            return true;
        }

        List<ChecklistItem> updatedCriteria = replace(acceptanceCriteria, itemId, done);

        if (updatedCriteria != null) {
            this.acceptanceCriteria = updatedCriteria;
            touch();
            return true;
        }

        return false;
    }

    public void linkRepository(UUID repositoryId) {
        this.repositoryId = repositoryId;
        this.branchName = null;
        touch();
    }

    public void unlinkRepository() {
        this.repositoryId = null;
        this.branchName = null;
        touch();
    }

    public void recordBranch(String branchName) {
        this.branchName = branchName;
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

    public List<ChecklistItem> getSubtasks() {
        return subtasks;
    }

    public List<ChecklistItem> getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public String getBranchName() {
        return branchName;
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
