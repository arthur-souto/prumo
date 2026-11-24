package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.AssignStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.ListWorkspaceMembersQuery;
import io.github.arthursouto.prumo.application.port.in.result.MemberView;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssigneeServicesTest {

    @Mock
    private WorkspaceRepositoryPort workspaceRepository;

    @Mock
    private StoryRepositoryPort storyRepository;

    @Mock
    private UserRepositoryPort userRepository;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private Workspace workspace;
    private UserStory story;
    private AssignStoryService assign;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWithMember(ownerId, memberId);
        Board board = BoardFixtures.boardFor(workspace);
        story = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "a");
        WorkspaceAccess access = new WorkspaceAccess(workspaceRepository);
        when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        lenient().when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        lenient().when(storyRepository.save(any(UserStory.class))).thenAnswer(call -> call.getArgument(0));
        assign = new AssignStoryService(new StoryLookup(access, storyRepository), access, storyRepository);
    }

    @Test
    void assignsToAMemberAndUnassigns() {
        assertThat(assign.execute(new AssignStoryCommand(ownerId, workspace.getId(), "PRU-1", memberId)).getAssigneeId())
                .isEqualTo(memberId);
        assertThat(assign.execute(new AssignStoryCommand(ownerId, workspace.getId(), "PRU-1", null)).getAssigneeId())
                .isNull();
    }

    @Test
    void rejectsAssigneesOutsideTheWorkspace() {
        assertThatThrownBy(() -> assign.execute(new AssignStoryCommand(ownerId, workspace.getId(), "PRU-1", UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(storyRepository, never()).save(any());
    }

    @Test
    void listsMembersWithTheirProfile() {
        User owner = User.registerExternal("Ada", "ada@example.com", "https://img/ada.png");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.findById(memberId)).thenReturn(Optional.empty());
        ListWorkspaceMembersService service = new ListWorkspaceMembersService(new WorkspaceAccess(workspaceRepository), userRepository);

        List<MemberView> members = service.execute(new ListWorkspaceMembersQuery(ownerId, workspace.getId()));

        assertThat(members).hasSize(1);
        assertThat(members.get(0).name()).isEqualTo("Ada");
        assertThat(members.get(0).avatarUrl()).isEqualTo("https://img/ada.png");
    }
}
