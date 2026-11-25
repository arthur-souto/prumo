package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.application.port.out.BoardRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SprintViews {

    private final BoardRepositoryPort boardRepository;
    private final StoryRepositoryPort storyRepository;

    public SprintViews(BoardRepositoryPort boardRepository, StoryRepositoryPort storyRepository) {
        this.boardRepository = boardRepository;
        this.storyRepository = storyRepository;
    }

    public List<SprintView> of(UUID workspaceId, List<Sprint> sprints) {
        if (sprints.isEmpty()) {
            return List.of();
        }

        Set<UUID> doneColumns = doneColumns(workspaceId);
        Map<UUID, List<UserStory>> bySprint = storyRepository.findAllBySprintIds(sprints.stream().map(Sprint::getId).toList()).stream()
                .filter(story -> story.getSprintId() != null)
                .collect(Collectors.groupingBy(UserStory::getSprintId));

        return sprints.stream()
                .map(sprint -> view(sprint, bySprint.getOrDefault(sprint.getId(), List.of()), doneColumns))
                .toList();
    }

    public SprintView of(Sprint sprint) {
        return of(sprint.getWorkspaceId(), List.of(sprint)).get(0);
    }

    public Set<UUID> doneColumns(UUID workspaceId) {
        return boardRepository.findByWorkspaceId(workspaceId)
                .map(board -> board.getColumns().stream()
                        .filter(column -> column.getKind() == ColumnKind.DONE)
                        .map(BoardColumn::getId)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    public static int points(List<UserStory> stories) {
        return stories.stream().map(UserStory::getStoryPoints).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    private static SprintView view(Sprint sprint, List<UserStory> stories, Set<UUID> doneColumns) {
        List<UserStory> done = stories.stream().filter(story -> doneColumns.contains(story.getColumnId())).toList();
        return new SprintView(sprint, stories.size(), points(stories), points(done));
    }
}
