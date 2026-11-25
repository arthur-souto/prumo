package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetVelocityUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetVelocityQuery;
import io.github.arthursouto.prumo.application.port.in.result.Velocity;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.query.SprintSearchCriteria;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
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

    public GetVelocityService(WorkspaceAccess access, SprintRepositoryPort sprintRepository) {
        this.access = access;
        this.sprintRepository = sprintRepository;
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

        List<Sprint> oldestFirst = new ArrayList<>(latest);
        Collections.reverse(oldestFirst);

        return new Velocity(
                oldestFirst,
                average(oldestFirst, Sprint::getCompletedPoints),
                average(oldestFirst, Sprint::getCommittedPoints)
        );
    }

    private static double average(List<Sprint> sprints, Function<Sprint, Integer> points) {
        double value = sprints.stream().map(points).filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(value * 10) / 10.0;
    }
}
