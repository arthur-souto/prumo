package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetSprintMetricsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetSprintQuery;
import io.github.arthursouto.prumo.application.port.in.result.BurndownDay;
import io.github.arthursouto.prumo.application.port.in.result.SprintMetrics;
import io.github.arthursouto.prumo.application.port.out.SprintSnapshotRepositoryPort;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintSnapshot;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GetSprintMetricsService implements GetSprintMetricsUseCase {

    private final SprintLookup sprints;
    private final SprintSnapshotRepositoryPort snapshotRepository;
    private final SprintSnapshots snapshots;

    public GetSprintMetricsService(SprintLookup sprints, SprintSnapshotRepositoryPort snapshotRepository, SprintSnapshots snapshots) {
        this.sprints = sprints;
        this.snapshotRepository = snapshotRepository;
        this.snapshots = snapshots;
    }

    @Override
    public SprintMetrics execute(GetSprintQuery query) {
        Sprint sprint = sprints.asMember(query.userId(), query.workspaceId(), query.sprintId());
        TreeMap<LocalDate, SprintSnapshot> byDay = snapshotRepository.findBySprintId(sprint.getId()).stream()
                .collect(Collectors.toMap(SprintSnapshot::date, Function.identity(), (first, second) -> second, TreeMap::new));

        if (sprint.isActive()) {
            SprintSnapshot now = snapshots.measure(sprint);
            byDay.put(now.date(), now);
        }

        LocalDate lastRecorded = lastRecordedDay(sprint);
        LocalDate lastDay = lastRecorded != null && lastRecorded.isAfter(sprint.getEndDate()) ? lastRecorded : sprint.getEndDate();
        int baseline = baseline(sprint, byDay);
        long span = Math.max(ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate()), 1);

        List<BurndownDay> days = new ArrayList<>();
        Map.Entry<LocalDate, SprintSnapshot> before = byDay.lowerEntry(sprint.getStartDate());
        SprintSnapshot known = before == null ? null : before.getValue();

        for (LocalDate day = sprint.getStartDate(); !day.isAfter(lastDay); day = day.plusDays(1)) {
            SprintSnapshot recorded = byDay.get(day);
            known = recorded != null ? recorded : known;
            boolean measured = lastRecorded != null && !day.isAfter(lastRecorded) && known != null;
            long elapsed = ChronoUnit.DAYS.between(sprint.getStartDate(), day);
            double ideal = Math.max(0, Math.round(baseline * (1 - (double) elapsed / span) * 100) / 100.0);

            days.add(new BurndownDay(
                    day,
                    measured ? known.totalPoints() : null,
                    measured ? known.remainingPoints() : null,
                    measured ? known.totalStories() : null,
                    measured ? known.remainingStories() : null,
                    ideal
            ));
        }

        return new SprintMetrics(sprint, baseline, days);
    }

    private LocalDate lastRecordedDay(Sprint sprint) {
        if (sprint.isActive()) {
            return snapshots.today();
        }

        if (sprint.isClosed() && sprint.getClosedAt() != null) {
            return snapshots.dayOf(sprint.getClosedAt());
        }

        return null;
    }

    private static int baseline(Sprint sprint, Map<LocalDate, SprintSnapshot> byDay) {
        if (sprint.getCommittedPoints() != null && sprint.getCommittedPoints() > 0) {
            return sprint.getCommittedPoints();
        }

        return byDay.values().stream().findFirst().map(SprintSnapshot::totalPoints).orElse(0);
    }
}
