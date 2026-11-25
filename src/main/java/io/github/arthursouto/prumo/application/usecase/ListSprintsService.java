package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListSprintsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListSprintsQuery;
import io.github.arthursouto.prumo.application.port.in.result.SprintView;
import io.github.arthursouto.prumo.application.port.out.SprintRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.query.SprintSearchCriteria;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;
import io.github.arthursouto.prumo.domain.model.sprint.SprintState;

import java.util.EnumSet;
import java.util.Set;

public class ListSprintsService implements ListSprintsUseCase {

    static final int DEFAULT_PAGE_SIZE = 20;

    private final WorkspaceAccess access;
    private final SprintRepositoryPort sprintRepository;
    private final SprintViews views;

    public ListSprintsService(WorkspaceAccess access, SprintRepositoryPort sprintRepository, SprintViews views) {
        this.access = access;
        this.sprintRepository = sprintRepository;
        this.views = views;
    }

    @Override
    public PageResult<SprintView> execute(ListSprintsQuery query) {
        access.member(query.userId(), query.workspaceId());

        String text = query.q() == null || query.q().isBlank() ? null : query.q().strip();
        Set<SprintState> states = query.states() == null || query.states().isEmpty()
                ? EnumSet.allOf(SprintState.class)
                : EnumSet.copyOf(query.states());

        PageResult<Sprint> page = sprintRepository.search(new SprintSearchCriteria(
                query.workspaceId(),
                text,
                states,
                query.page() == null ? 0 : query.page(),
                query.size() == null ? DEFAULT_PAGE_SIZE : query.size()
        ));

        return new PageResult<>(views.of(query.workspaceId(), page.items()), page.page(), page.size(), page.totalItems());
    }
}
