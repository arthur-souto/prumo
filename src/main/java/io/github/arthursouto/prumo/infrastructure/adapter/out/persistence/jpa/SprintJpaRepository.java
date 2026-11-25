package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.domain.model.sprint.SprintState;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.SprintEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SprintJpaRepository extends JpaRepository<SprintEntity, UUID> {

    String MATCHES = """
            s.workspaceId = :workspaceId
              and s.state in :states
              and (cast(function('immutable_unaccent', lower(s.name)) as String)
                    like cast(function('immutable_unaccent', :pattern) as String) escape '\\'
                or cast(function('immutable_unaccent', lower(coalesce(s.goal, ''))) as String)
                    like cast(function('immutable_unaccent', :pattern) as String) escape '\\')
            """;

    @Query(value = """
            select s from SprintEntity s
            where
            """ + MATCHES + """
            order by
              case s.state
                when io.github.arthursouto.prumo.domain.model.sprint.SprintState.ACTIVE then 0
                when io.github.arthursouto.prumo.domain.model.sprint.SprintState.PLANNED then 1
                else 2
              end,
              case when s.state = io.github.arthursouto.prumo.domain.model.sprint.SprintState.CLOSED then null else s.startDate end asc,
              s.startDate desc,
              s.createdAt desc
            """,
            countQuery = "select count(s) from SprintEntity s where\n" + MATCHES)
    Page<SprintEntity> search(@Param("workspaceId") UUID workspaceId,
                              @Param("states") Collection<SprintState> states,
                              @Param("pattern") String pattern,
                              Pageable pageable);

    Optional<SprintEntity> findByWorkspaceIdAndState(UUID workspaceId, SprintState state);
}
