package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.CloseSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.command.SprintCommand;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CloseSprintService implements CloseSprintUseCase {

    private final SprintLookup sprints;
    private final SprintRepositoryPort sprintRepository;
    private final StoryRepositoryPort storyRepository;
    private final SprintViews views;
    private final SprintSnapshots snapshots;

    public CloseSprintService(SprintLookup sprints,
                              SprintRepositoryPort sprintRepository,
                              StoryRepositoryPort storyRepository,
                              SprintViews views,
                              SprintSnapshots snapshots) {
        this.sprints = sprints;
        this.sprintRepository = sprintRepository;
        this.storyRepository = storyRepository;
        this.views = views;
        this.snapshots = snapshots;
    }

    @Override
    public SprintView execute(SprintCommand command) {
        Sprint sprint = sprints.asEditor(command.userId(), command.workspaceId(), command.sprintId());

        if (!sprint.isActive()) {
            throw new BusinessRuleException("Only the active sprint can be closed");
        }

        snapshots.record(sprint);

        Set<UUID> doneColumns = views.doneColumns(command.workspaceId());
        Map<Boolean, List<UserStory>> stories = storyRepository.findAllBySprintIds(List.of(sprint.getId())).stream()
                .collect(Collectors.partitioningBy(story -> doneColumns.contains(story.getColumnId())));
        List<UserStory> unfinished = stories.get(false);

        unfinished.forEach(story -> story.planInto(null));
        if (!unfinished.isEmpty()) {
            storyRepository.saveAll(unfinished);
        }
        sprint.close(SprintViews.points(stories.get(true)));

        return views.of(sprintRepository.save(sprint));
    }
}
