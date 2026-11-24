package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import io.github.arthursouto.prumo.domain.model.workspace.WorkspaceMember;
import io.github.arthursouto.prumo.domain.model.workspace.WorkspaceRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class BoardFixtures {

    private BoardFixtures() {
    }

    static Workspace workspaceWith(UUID ownerId) {
        return Workspace.create("Prumo", "prumo", null, ownerId);
    }

    static Workspace workspaceWithMember(UUID ownerId, UUID memberId) {
        Workspace base = workspaceWith(ownerId);
        List<WorkspaceMember> members = new java.util.ArrayList<>(base.getMembers());
        members.add(WorkspaceMember.restore(UUID.randomUUID(), base.getId(), memberId, WorkspaceRole.MEMBER, Instant.now()));

        return Workspace.restore(
                base.getId(),
                base.getName(),
                base.getSlug(),
                base.getDescription(),
                ownerId,
                members,
                base.getCreatedAt(),
                base.getUpdatedAt()
        );
    }

    static Board boardFor(Workspace workspace) {
        return Board.withDefaults(workspace.getId(), "PRU");
    }

    static BoardColumn column(Board board, ColumnKind kind) {
        return board.ofKind(kind).orElseThrow();
    }

    static UserStory story(Workspace workspace, BoardColumn column, int number, String rank) {
        return UserStory.create(
                workspace.getId(),
                number,
                "PRU-" + number,
                "Story " + number,
                null,
                StoryPriority.MEDIUM,
                null,
                column.getId(),
                rank,
                List.of(),
                List.of()
        );
    }
}
