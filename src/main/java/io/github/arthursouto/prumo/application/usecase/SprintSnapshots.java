package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.SprintSnapshotRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintSnapshot;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SprintSnapshots {

    private final StoryRepositoryPort storyRepository;
    private final SprintSnapshotRepositoryPort snapshotRepository;
    private final SprintViews views;
    private final Clock clock;
    private final ZoneId zone;

    public SprintSnapshots(StoryRepositoryPort storyRepository,
                           SprintSnapshotRepositoryPort snapshotRepository,
                           SprintViews views,
                           Clock clock,
                           ZoneId zone) {
        this.storyRepository = storyRepository;
        this.snapshotRepository = snapshotRepository;
        this.views = views;
        this.clock = clock;
        this.zone = zone;
    }

    public SprintSnapshot record(Sprint sprint) {
        SprintSnapshot snapshot = measure(sprint);
        snapshotRepository.saveForDay(snapshot);

        return snapshot;
    }

    public SprintSnapshot measure(Sprint sprint) {
        Set<UUID> doneColumns = views.doneColumns(sprint.getWorkspaceId());
        List<UserStory> stories = storyRepository.findAllBySprintIds(List.of(sprint.getId()));
        List<UserStory> remaining = stories.stream().filter(story -> !doneColumns.contains(story.getColumnId())).toList();

        return SprintSnapshot.of(
                sprint.getId(),
                today(),
                SprintViews.points(stories),
                SprintViews.points(remaining),
                stories.size(),
                remaining.size(),
                clock.instant()
        );
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }

    public LocalDate dayOf(Instant instant) {
        return LocalDate.ofInstant(instant, zone);
    }
}
