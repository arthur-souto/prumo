package io.github.arthursouto.prumo.domain.model.board;

import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class Board {

    private final UUID workspaceId;
    private String keyPrefix;
    private boolean automationEnabled;
    private final List<BoardColumn> columns;

    private Board(UUID workspaceId, String keyPrefix, boolean automationEnabled, List<BoardColumn> columns) {
        this.workspaceId = workspaceId;
        this.keyPrefix = keyPrefix;
        this.automationEnabled = automationEnabled;
        this.columns = new ArrayList<>(columns);
        this.columns.sort(Comparator.comparingInt(BoardColumn::getPosition));
    }

    public static Board of(UUID workspaceId, String keyPrefix, boolean automationEnabled, List<BoardColumn> columns) {
        return new Board(workspaceId, keyPrefix, automationEnabled, columns);
    }

    public static Board withDefaults(UUID workspaceId, String keyPrefix) {
        return new Board(workspaceId, keyPrefix, false, List.of(
                BoardColumn.create(workspaceId, "To Do", ColumnKind.TODO, 0),
                BoardColumn.create(workspaceId, "In Progress", ColumnKind.IN_PROGRESS, 1),
                BoardColumn.create(workspaceId, "In Review", ColumnKind.IN_REVIEW, 2),
                BoardColumn.create(workspaceId, "Done", ColumnKind.DONE, 3)
        ));
    }

    public void changeKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public void setAutomationEnabled(boolean automationEnabled) {
        this.automationEnabled = automationEnabled;
    }

    public BoardColumn addColumn(String name) {
        requireUniqueName(name, null);
        BoardColumn column = BoardColumn.create(workspaceId, name, ColumnKind.CUSTOM, columns.size());
        columns.add(column);
        return column;
    }

    public BoardColumn rename(UUID columnId, String name) {
        BoardColumn column = require(columnId);
        requireUniqueName(name, columnId);
        column.rename(name);
        return column;
    }

    public List<BoardColumn> reorder(List<UUID> orderedIds) {
        Set<UUID> current = new HashSet<>();
        columns.forEach(column -> current.add(column.getId()));

        if (orderedIds.size() != columns.size() || !current.equals(new HashSet<>(orderedIds))) {
            throw new BusinessRuleException("The new order must contain every column exactly once");
        }

        List<BoardColumn> reordered = new ArrayList<>();
        orderedIds.forEach(id -> reordered.add(require(id)));
        columns.clear();
        columns.addAll(reordered);
        renumber();

        return List.copyOf(columns);
    }

    public BoardColumn remove(UUID columnId, boolean hasStories, UUID moveToColumnId) {
        BoardColumn column = require(columnId);

        if (columns.size() == 1) {
            throw new BusinessRuleException("A board needs at least one column");
        }

        if (hasStories && moveToColumnId == null) {
            throw new BusinessRuleException("Move the stories to another column before removing this one");
        }

        if (moveToColumnId != null && (moveToColumnId.equals(columnId) || find(moveToColumnId).isEmpty())) {
            throw new BusinessRuleException("The destination column is invalid");
        }

        columns.remove(column);
        renumber();

        return column;
    }

    public Optional<BoardColumn> find(UUID columnId) {
        return columns.stream().filter(column -> column.getId().equals(columnId)).findFirst();
    }

    public BoardColumn require(UUID columnId) {
        return find(columnId).orElseThrow(() -> new ResourceNotFoundException("Column not found"));
    }

    public BoardColumn first() {
        return columns.get(0);
    }

    public Optional<BoardColumn> ofKind(ColumnKind kind) {
        return columns.stream().filter(column -> column.getKind() == kind).findFirst();
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public boolean isAutomationEnabled() {
        return automationEnabled;
    }

    public List<BoardColumn> getColumns() {
        return List.copyOf(columns);
    }

    private void renumber() {
        for (int position = 0; position < columns.size(); position++) {
            columns.get(position).moveTo(position);
        }
    }

    private void requireUniqueName(String name, UUID ignoredId) {
        String normalized = name.strip().toLowerCase(Locale.ROOT);
        boolean taken = columns.stream()
                .filter(column -> !column.getId().equals(ignoredId))
                .anyMatch(column -> column.getName().strip().toLowerCase(Locale.ROOT).equals(normalized));

        if (taken) {
            throw new ConflictException("A column with this name already exists");
        }
    }
}
