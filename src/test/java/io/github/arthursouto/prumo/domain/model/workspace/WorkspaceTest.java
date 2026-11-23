package io.github.arthursouto.prumo.domain.model.workspace;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceTest {

    @Test
    void createMakesTheCreatorTheOwnerAndOnlyMember() {
        UUID ownerId = UUID.randomUUID();

        Workspace workspace = Workspace.create("Acme", "acme", "Desc", ownerId);

        assertThat(workspace.getId()).isNotNull();
        assertThat(workspace.getName()).isEqualTo("Acme");
        assertThat(workspace.getSlug()).isEqualTo("acme");
        assertThat(workspace.getDescription()).isEqualTo("Desc");
        assertThat(workspace.getOwnerId()).isEqualTo(ownerId);
        assertThat(workspace.getCreatedAt()).isEqualTo(workspace.getUpdatedAt());
        assertThat(workspace.getMembers()).hasSize(1);
        assertThat(workspace.getMembers().get(0).getWorkspaceId()).isEqualTo(workspace.getId());
        assertThat(workspace.roleOf(ownerId)).contains(WorkspaceRole.OWNER);
    }

    @Test
    void updateChangesNameAndDescriptionButKeepsTheSlug() {
        Workspace workspace = Workspace.create("Acme", "acme", "Old", UUID.randomUUID());

        workspace.update("New", null);

        assertThat(workspace.getName()).isEqualTo("New");
        assertThat(workspace.getDescription()).isNull();
        assertThat(workspace.getSlug()).isEqualTo("acme");
        assertThat(workspace.getUpdatedAt()).isAfterOrEqualTo(workspace.getCreatedAt());
    }

    @Test
    void ownerCanEditAndDeleteWhileStrangersCannot() {
        Workspace workspace = Workspace.create("Acme", "acme", null, UUID.randomUUID());

        assertThat(workspace.canEdit(workspace.getOwnerId())).isTrue();
        assertThat(workspace.canDelete(workspace.getOwnerId())).isTrue();
        assertThat(workspace.canEdit(UUID.randomUUID())).isFalse();
        assertThat(workspace.canDelete(UUID.randomUUID())).isFalse();
    }

    @Test
    void membershipQueriesOnlyKnowTheMembers() {
        Workspace workspace = Workspace.create("Acme", "acme", null, UUID.randomUUID());
        UUID stranger = UUID.randomUUID();

        assertThat(workspace.hasMember(workspace.getOwnerId())).isTrue();
        assertThat(workspace.hasMember(stranger)).isFalse();
        assertThat(workspace.roleOf(stranger)).isEmpty();
    }
}
