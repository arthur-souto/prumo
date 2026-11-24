package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListSubtasksUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListSubtasksQuery;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.List;

public class ListSubtasksService implements ListSubtasksUseCase {

    private final StoryLookup stories;
    private final SubtaskLookup lookup;
    private final SubtaskRepositoryPort subtaskRepository;

    public ListSubtasksService(StoryLookup stories, SubtaskLookup lookup, SubtaskRepositoryPort subtaskRepository) {
        this.stories = stories;
        this.lookup = lookup;
        this.subtaskRepository = subtaskRepository;
    }

    @Override
    public List<SubtaskView> execute(ListSubtasksQuery query) {
        UserStory story = stories.asMember(query.userId(), query.workspaceId(), query.storyKey());

        return subtaskRepository.findAllByStoryId(story.getId()).stream()
                .map(subtask -> lookup.view(subtask, story))
                .toList();
    }
}
