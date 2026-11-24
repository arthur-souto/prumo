package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListStoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListStoriesQuery;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ListStoriesService implements ListStoriesUseCase {

    private final WorkspaceAccess access;
    private final StoryRepositoryPort storyRepository;

    public ListStoriesService(WorkspaceAccess access, StoryRepositoryPort storyRepository) {
        this.access = access;
        this.storyRepository = storyRepository;
    }

    @Override
    public List<UserStory> execute(ListStoriesQuery query) {
        access.member(query.userId(), query.workspaceId());
        String text = query.text() == null ? "" : query.text().strip().toLowerCase(Locale.ROOT);
        String label = query.label() == null ? null : query.label().strip().toLowerCase(Locale.ROOT);

        return storyRepository.findAllByWorkspaceId(query.workspaceId()).stream()
                .filter(story -> query.priority() == null || story.getPriority() == query.priority())
                .filter(story -> label == null || label.isEmpty() || story.getLabels().stream()
                        .anyMatch(candidate -> candidate.toLowerCase(Locale.ROOT).equals(label)))
                .filter(story -> text.isEmpty() || matches(story, text))
                .sorted(Comparator.comparingInt(UserStory::getNumber).reversed())
                .toList();
    }

    private static boolean matches(UserStory story, String text) {
        return story.getTitle().toLowerCase(Locale.ROOT).contains(text)
                || story.getKey().toLowerCase(Locale.ROOT).contains(text)
                || (story.getDescription() != null && story.getDescription().toLowerCase(Locale.ROOT).contains(text));
    }
}
