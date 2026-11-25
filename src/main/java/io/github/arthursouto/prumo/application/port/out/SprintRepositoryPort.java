package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.application.port.out.query.SprintSearchCriteria;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.model.sprint.Sprint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SprintRepositoryPort {

    Sprint save(Sprint sprint);

    Optional<Sprint> findById(UUID id);

    PageResult<Sprint> search(SprintSearchCriteria criteria);

    Optional<Sprint> findActive(UUID workspaceId);

    List<Sprint> findAllActive();

    void delete(Sprint sprint);
}
