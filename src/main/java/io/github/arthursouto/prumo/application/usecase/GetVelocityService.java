package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetVelocityUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetVelocityQuery;
import io.github.arthursouto.prumo.application.port.in.result.Velocity;
import io.github.arthursouto.prumo.application.port.in.result.VelocitySprint;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SprintSnapshotRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.query.SprintSearchCriteria;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintSnapshot;
import io.github.arthursouto.prumo.domain.model.sprint.SprintState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class GetVelocityService implements GetVelocityUseCase {

    static final int DEFAULT_LIMIT = 6;

    private final WorkspaceAccess access;
    private final SprintRepositoryPort sprintRepository;
    private final SprintSnapshotRepositoryPort snapshotRepository;

    public GetVelocityService(WorkspaceAccess access, SprintRepositoryPort sprintRepository, SprintSnapshotRepositoryPort snapshotRepository) {
        this.access = access;
        this.sprintRepository = sprintRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    public Velocity execute(GetVelocityQuery query) {
        access.member(query.userId(), query.workspaceId());

        int limit = query.limit() == null ? DEFAULT_LIMIT : query.limit();
        List<Sprint> latest = sprintRepository.search(new SprintSearchCriteria(
                query.workspaceId(),
                null,
                Set.of(SprintState.CLOSED),
                0,
                limit
        )).items();

        List<VelocitySprint> oldestFirst = new ArrayList<>(latest.stream().map(this::entry).toList());
        Collections.reverse(oldestFirst);

        return new Velocity(
                oldestFirst,
                average(oldestFirst, entry -> entry.sprint().getCompletedPoints()),
                average(oldestFirst, VelocitySprint::committedPoints)
        );
    }

    private VelocitySprint entry(Sprint sprint) {
        Integer committed = sprint.getCommittedPoints();

        if (committed != null && committed > 0) {
            return new VelocitySprint(sprint, committed);
        }

        Integer firstTotal = snapshotRepository.findBySprintId(sprint.getId()).stream()
                .findFirst()
                .map(SprintSnapshot::totalPoints)
                .orElse(committed);

        return new VelocitySprint(sprint, firstTotal);
    }

    private static double average(List<VelocitySprint> sprints, Function<VelocitySprint, Integer> points) {
        double value = sprints.stream().map(points).filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(value * 10) / 10.0;
    }
}
