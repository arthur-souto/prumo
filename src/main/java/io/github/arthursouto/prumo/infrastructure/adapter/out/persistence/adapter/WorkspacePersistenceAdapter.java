package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.exceptions.WorkspaceSlugTakenException;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.WorkspaceEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.WorkspaceMemberEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.WorkspaceJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.WorkspaceMemberJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.WorkspaceMapper;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class WorkspacePersistenceAdapter implements WorkspaceRepositoryPort {

    private static final String SLUG_CONSTRAINT = "uq_workspaces_slug";

    private final WorkspaceJpaRepository workspaceRepository;
    private final WorkspaceMemberJpaRepository memberRepository;

    public WorkspacePersistenceAdapter(WorkspaceJpaRepository workspaceRepository,
                                       WorkspaceMemberJpaRepository memberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public Workspace save(Workspace workspace) {
        WorkspaceEntity saved;

        try {
            saved = workspaceRepository.saveAndFlush(WorkspaceMapper.toEntity(workspace));
        } catch (DataIntegrityViolationException exception) {
            String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(exception).getMessage());

            if (cause.contains(SLUG_CONSTRAINT)) {
                throw new WorkspaceSlugTakenException();
            }

            throw exception;
        }

        List<WorkspaceMemberEntity> members = memberRepository.saveAll(
                workspace.getMembers().stream().map(WorkspaceMapper::toEntity).toList()
        );

        return WorkspaceMapper.toDomain(saved, members);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Workspace> findById(UUID id) {
        return workspaceRepository.findById(id)
                .map(entity -> WorkspaceMapper.toDomain(entity, memberRepository.findByWorkspaceId(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workspace> findAllByMemberUserId(UUID userId) {
        Set<UUID> workspaceIds = memberRepository.findByUserId(userId).stream()
                .map(WorkspaceMemberEntity::getWorkspaceId)
                .collect(Collectors.toSet());

        if (workspaceIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<WorkspaceMemberEntity>> membersByWorkspace = memberRepository
                .findByWorkspaceIdIn(workspaceIds)
                .stream()
                .collect(Collectors.groupingBy(WorkspaceMemberEntity::getWorkspaceId));

        return workspaceRepository.findByIdInOrderByCreatedAtDesc(workspaceIds).stream()
                .map(entity -> WorkspaceMapper.toDomain(
                        entity,
                        membersByWorkspace.getOrDefault(entity.getId(), List.of())
                ))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        workspaceRepository.deleteById(id);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return workspaceRepository.existsBySlug(slug);
    }
}
