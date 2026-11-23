package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.exceptions.WorkspaceSlugTakenException;
import io.github.arthursouto.prumo.application.port.in.command.CreateWorkspaceCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteWorkspaceCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetWorkspaceQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListWorkspacesQuery;
import io.github.arthursouto.prumo.application.port.in.command.UpdateWorkspaceCommand;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import io.github.arthursouto.prumo.domain.model.workspace.WorkspaceMember;
import io.github.arthursouto.prumo.domain.model.workspace.WorkspaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServicesTest {

    @Mock
    private WorkspaceRepositoryPort repository;

    private final UUID userId = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID member = UUID.randomUUID();
    private CreateWorkspaceService createService;

    private Workspace withMembers() {
        Workspace base = Workspace.create("Acme", "acme", null, userId);
        Instant now = Instant.now();

        return Workspace.restore(
                base.getId(),
                base.getName(),
                base.getSlug(),
                base.getDescription(),
                userId,
                List.of(
                        base.getMembers().get(0),
                        WorkspaceMember.restore(UUID.randomUUID(), base.getId(), admin, WorkspaceRole.ADMIN, now),
                        WorkspaceMember.restore(UUID.randomUUID(), base.getId(), member, WorkspaceRole.MEMBER, now)
                ),
                base.getCreatedAt(),
                base.getUpdatedAt()
        );
    }

    @BeforeEach
    void setUp() {
        createService = new CreateWorkspaceService(repository);
    }

    @Test
    void createStripsTheInputBuildsTheSlugAndMakesTheCallerOwner() {
        when(repository.existsBySlug("acme-engineering")).thenReturn(false);
        when(repository.save(any(Workspace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Workspace workspace = createService.execute(new CreateWorkspaceCommand(userId, "  Acme Engineering ", " Desc "));

        ArgumentCaptor<Workspace> captor = ArgumentCaptor.forClass(Workspace.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(workspace);
        assertThat(workspace.getName()).isEqualTo("Acme Engineering");
        assertThat(workspace.getSlug()).isEqualTo("acme-engineering");
        assertThat(workspace.getDescription()).isEqualTo("Desc");
        assertThat(workspace.roleOf(userId)).contains(WorkspaceRole.OWNER);
    }

    @Test
    void createStoresABlankDescriptionAsNull() {
        when(repository.existsBySlug("acme")).thenReturn(false);
        when(repository.save(any(Workspace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(createService.execute(new CreateWorkspaceCommand(userId, "Acme", "  ")).getDescription()).isNull();
        assertThat(createService.execute(new CreateWorkspaceCommand(userId, "Acme", null)).getDescription()).isNull();
    }

    @Test
    void createAddsANumericSuffixWhileTheSlugIsTaken() {
        when(repository.existsBySlug("acme")).thenReturn(true);
        when(repository.existsBySlug("acme-2")).thenReturn(true);
        when(repository.existsBySlug("acme-3")).thenReturn(false);
        when(repository.save(any(Workspace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(createService.execute(new CreateWorkspaceCommand(userId, "Acme", null)).getSlug())
                .isEqualTo("acme-3");
    }

    @Test
    void slugifyRemovesAccentsSymbolsAndLimitsTheLength() {
        assertThat(CreateWorkspaceService.slugify("Ação & Não--Café!")).isEqualTo("acao-nao-cafe");
        assertThat(CreateWorkspaceService.slugify("***")).isEqualTo("workspace");
        assertThat(CreateWorkspaceService.slugify("a".repeat(80))).hasSize(50);
        assertThat(CreateWorkspaceService.slugify("a".repeat(49) + " b")).isEqualTo("a".repeat(49));
    }

    @Test
    void createRetriesWithAnotherSlugWhenTheSlugIsTakenConcurrently() {
        when(repository.existsBySlug("acme")).thenReturn(false, true);
        when(repository.existsBySlug("acme-2")).thenReturn(false);
        when(repository.save(any(Workspace.class)))
                .thenThrow(new WorkspaceSlugTakenException())
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(createService.execute(new CreateWorkspaceCommand(userId, "Acme", null)).getSlug())
                .isEqualTo("acme-2");
    }

    @Test
    void createGivesUpAfterRepeatedSlugCollisions() {
        when(repository.save(any(Workspace.class))).thenThrow(new WorkspaceSlugTakenException());

        assertThatThrownBy(() -> createService.execute(new CreateWorkspaceCommand(userId, "Acme", null)))
                .isInstanceOf(ConflictException.class);
        verify(repository, times(5)).save(any(Workspace.class));
    }

    @Test
    void ownersAndAdminsCanEditButMembersCannot() {
        Workspace workspace = withMembers();
        when(repository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(repository.save(any(Workspace.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateWorkspaceService service = new UpdateWorkspaceService(repository);

        Workspace updated = service.execute(new UpdateWorkspaceCommand(admin, workspace.getId(), "  New name ", "  "));

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getSlug()).isEqualTo("acme");
        assertThat(service.execute(new UpdateWorkspaceCommand(userId, workspace.getId(), "Owner edit", "d")).getName())
                .isEqualTo("Owner edit");
        assertThatThrownBy(() -> service.execute(new UpdateWorkspaceCommand(member, workspace.getId(), "x", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateHidesTheWorkspaceFromNonMembers() {
        Workspace workspace = withMembers();
        when(repository.findById(workspace.getId())).thenReturn(Optional.of(workspace));

        assertThatThrownBy(() -> new UpdateWorkspaceService(repository)
                .execute(new UpdateWorkspaceCommand(UUID.randomUUID(), workspace.getId(), "x", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(Workspace.class));
    }

    @Test
    void onlyTheOwnerCanDelete() {
        Workspace workspace = withMembers();
        when(repository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        DeleteWorkspaceService service = new DeleteWorkspaceService(repository);

        assertThatThrownBy(() -> service.execute(new DeleteWorkspaceCommand(admin, workspace.getId())))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.execute(new DeleteWorkspaceCommand(UUID.randomUUID(), workspace.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).deleteById(any(UUID.class));

        service.execute(new DeleteWorkspaceCommand(userId, workspace.getId()));

        verify(repository).deleteById(workspace.getId());
    }

    @Test
    void listReturnsTheWorkspacesOfTheUser() {
        List<Workspace> workspaces = List.of(Workspace.create("Acme", "acme", null, userId));
        when(repository.findAllByMemberUserId(userId)).thenReturn(workspaces);

        assertThat(new ListWorkspacesService(repository).execute(new ListWorkspacesQuery(userId)))
                .isEqualTo(workspaces);
    }

    @Test
    void getReturnsTheWorkspaceToAMember() {
        Workspace workspace = Workspace.create("Acme", "acme", null, userId);
        when(repository.findById(workspace.getId())).thenReturn(Optional.of(workspace));

        assertThat(new GetWorkspaceService(repository).execute(new GetWorkspaceQuery(userId, workspace.getId())))
                .isSameAs(workspace);
    }

    @Test
    void getHidesTheWorkspaceFromNonMembersAndMissingIds() {
        Workspace workspace = Workspace.create("Acme", "acme", null, UUID.randomUUID());
        when(repository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());
        GetWorkspaceService service = new GetWorkspaceService(repository);

        assertThatThrownBy(() -> service.execute(new GetWorkspaceQuery(userId, workspace.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.execute(new GetWorkspaceQuery(userId, missing)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
