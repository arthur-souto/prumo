package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListStoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListStoriesQuery;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.query.SortDirection;
import io.github.arthursouto.prumo.application.port.out.query.StorySearchCriteria;
import io.github.arthursouto.prumo.application.port.out.query.StorySort;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ListStoriesService implements ListStoriesUseCase {

    static final int DEFAULT_PAGE_SIZE = 20;

    private final WorkspaceAccess access;
    private final StoryRepositoryPort storyRepository;

    public ListStoriesService(WorkspaceAccess access, StoryRepositoryPort storyRepository) {
        this.access = access;
        this.storyRepository = storyRepository;
    }

    @Override
    public PageResult<UserStory> execute(ListStoriesQuery query) {
        access.member(query.userId(), query.workspaceId());

        return storyRepository.search(new StorySearchCriteria(
                query.workspaceId(),
                text(query.text()),
                labels(query.labels()),
                distinct(query.priorities()),
                distinct(query.columnIds()),
                distinct(query.assigneeIds()),
                Boolean.TRUE.equals(query.unassigned()),
                query.sort() == null ? StorySort.NUMBER : query.sort(),
                query.direction() == null ? SortDirection.DESC : query.direction(),
                query.page() == null ? 0 : query.page(),
                query.size() == null ? DEFAULT_PAGE_SIZE : query.size()
        ));
    }

    private static String text(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.strip().toLowerCase(Locale.ROOT);
    }

    private static Set<String> labels(List<String> labels) {
        if (labels == null) {
            return Set.of();
        }
        return labels.stream()
                .filter(Objects::nonNull)
                .map(label -> label.strip().toLowerCase(Locale.ROOT))
                .filter(label -> !label.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <T> Set<T> distinct(List<T> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
