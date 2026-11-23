package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.application.exceptions.WorkspaceSlugTakenException;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import io.github.arthursouto.prumo.domain.model.workspace.WorkspaceRole;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter.WorkspacePersistenceAdapter;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.WorkspaceEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.WorkspaceMemberEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.WorkspaceJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.WorkspaceMemberJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.WorkspaceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspacePersistenceTest {

    @Mock
    private WorkspaceJpaRepository workspaceRepository;

    @Mock
    private WorkspaceMemberJpaRepository memberRepository;

    private WorkspacePersistenceAdapter adapter;
    private UUID userId;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        adapter = new WorkspacePersistenceAdapter(workspaceRepository, memberRepository);
        userId = UUID.randomUUID();
        workspace = Workspace.create("Acme", "acme", "Desc", userId);
    }

    @Test
    void mapperRoundTripPreservesTheWorkspaceAndItsMembers() {
        WorkspaceEntity entity = WorkspaceMapper.toEntity(workspace);
        List<WorkspaceMemberEntity> members = workspace.getMembers().stream().map(WorkspaceMapper::toEntity).toList();

        Workspace restored = WorkspaceMapper.toDomain(entity, members);

        assertThat(members.get(0).getRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(restored).usingRecursiveComparison().isEqualTo(workspace);
    }

    @Test
    void savePersistsTheWorkspaceAndItsMembers() {
        when(workspaceRepository.saveAndFlush(any(WorkspaceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adapter.save(workspace)).usingRecursiveComparison().isEqualTo(workspace);
    }

    @Test
    void findByIdLoadsTheMembers() {
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(WorkspaceMapper.toEntity(workspace)));
        when(memberRepository.findByWorkspaceId(workspace.getId()))
                .thenReturn(workspace.getMembers().stream().map(WorkspaceMapper::toEntity).toList());
        UUID missing = UUID.randomUUID();
        when(workspaceRepository.findById(missing)).thenReturn(Optional.empty());

        assertThat(adapter.findById(workspace.getId())).get().usingRecursiveComparison().isEqualTo(workspace);
        assertThat(adapter.findById(missing)).isEmpty();
    }

    @Test
    void findAllByMemberUserIdGroupsMembersPerWorkspace() {
        List<WorkspaceMemberEntity> members = workspace.getMembers().stream().map(WorkspaceMapper::toEntity).toList();
        when(memberRepository.findByUserId(userId)).thenReturn(members);
        when(memberRepository.findByWorkspaceIdIn(Set.of(workspace.getId()))).thenReturn(members);
        when(workspaceRepository.findByIdInOrderByCreatedAtDesc(Set.of(workspace.getId())))
                .thenReturn(List.of(WorkspaceMapper.toEntity(workspace)));

        assertThat(adapter.findAllByMemberUserId(userId)).hasSize(1);
        assertThat(adapter.findAllByMemberUserId(userId).get(0).roleOf(userId)).contains(WorkspaceRole.OWNER);
    }

    @Test
    void findAllByMemberUserIdIsEmptyWithoutMemberships() {
        when(memberRepository.findByUserId(userId)).thenReturn(List.of());

        assertThat(adapter.findAllByMemberUserId(userId)).isEmpty();
        verifyNoInteractions(workspaceRepository);
    }

    @Test
    void saveTurnsASlugViolationIntoASlugTakenException() {
        when(workspaceRepository.saveAndFlush(any(WorkspaceEntity.class)))
                .thenThrow(new DataIntegrityViolationException("x", new RuntimeException("duplicate key uq_workspaces_slug")));

        assertThatThrownBy(() -> adapter.save(workspace)).isInstanceOf(WorkspaceSlugTakenException.class);
    }

    @Test
    void saveKeepsOtherIntegrityViolationsUntouched() {
        when(workspaceRepository.saveAndFlush(any(WorkspaceEntity.class)))
                .thenThrow(new DataIntegrityViolationException("x", new RuntimeException("fk_owner violated")));

        assertThatThrownBy(() -> adapter.save(workspace)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteByIdDelegates() {
        adapter.deleteById(workspace.getId());

        verify(workspaceRepository).deleteById(workspace.getId());
    }

    @Test
    void existsBySlugDelegates() {
        when(workspaceRepository.existsBySlug("acme")).thenReturn(true);

        assertThat(adapter.existsBySlug("acme")).isTrue();
        assertThat(adapter.existsBySlug("other")).isFalse();
    }
}
