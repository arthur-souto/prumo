package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.command.AddCommentCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteCommentCommand;
import io.github.arthursouto.prumo.application.port.in.command.EditCommentCommand;
import io.github.arthursouto.prumo.application.port.in.command.ListCommentsQuery;
import io.github.arthursouto.prumo.application.port.in.result.CommentView;
import io.github.arthursouto.prumo.application.port.out.CommentRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.StoryRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.WorkspaceRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.ColumnKind;
import io.github.arthursouto.prumo.domain.model.comment.Comment;
import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServicesTest {

    @Mock
    private WorkspaceRepositoryPort workspaceRepository;

    @Mock
    private StoryRepositoryPort storyRepository;

    @Mock
    private SubtaskRepositoryPort subtaskRepository;

    @Mock
    private CommentRepositoryPort commentRepository;

    @Mock
    private UserRepositoryPort userRepository;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private Workspace workspace;
    private UserStory story;
    private Subtask subtask;
    private User ada;
    private StoryLookup stories;
    private SubtaskLookup subtasks;
    private CommentAuthors authors;
    private CommentAccess commentAccess;

    @BeforeEach
    void setUp() {
        workspace = BoardFixtures.workspaceWithMember(ownerId, memberId);
        Board board = BoardFixtures.boardFor(workspace);
        story = BoardFixtures.story(workspace, BoardFixtures.column(board, ColumnKind.TODO), 1, "a");
        subtask = Subtask.create(story.getId(), workspace.getId(), 2, "PRU-2", "Task", null, 0);
        ada = User.registerExternal("Ada", "ada@example.com", "https://img/ada.png");
        WorkspaceAccess access = new WorkspaceAccess(workspaceRepository);
        stories = new StoryLookup(access, storyRepository);
        subtasks = new SubtaskLookup(access, subtaskRepository, storyRepository);
        authors = new CommentAuthors(userRepository);
        commentAccess = new CommentAccess(access, commentRepository);
        lenient().when(workspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        lenient().when(storyRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-1")).thenReturn(Optional.of(story));
        lenient().when(subtaskRepository.findByWorkspaceIdAndKey(workspace.getId(), "PRU-2")).thenReturn(Optional.of(subtask));
        lenient().when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(userRepository.findById(ada.getId())).thenReturn(Optional.of(ada));
    }

    private AddCommentService adder() {
        return new AddCommentService(stories, subtasks, commentRepository, authors);
    }

    private Comment existing(UUID author, String body) {
        Comment comment = Comment.create(workspace.getId(), story.getId(), null, author, body);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        return comment;
    }

    @Test
    void aMemberCommentsOnAStoryAndTheTextIsTrimmed() {
        CommentView view = adder().execute(new AddCommentCommand(memberId, workspace.getId(), "pru-1", null, "  **Decided** to keep it \n"));

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(saved.capture());
        assertThat(saved.getValue().getStoryId()).isEqualTo(story.getId());
        assertThat(saved.getValue().getSubtaskKey()).isNull();
        assertThat(saved.getValue().getAuthorId()).isEqualTo(memberId);
        assertThat(saved.getValue().getBody()).isEqualTo("**Decided** to keep it");
        assertThat(saved.getValue().getEditedAt()).isNull();
        assertThat(view.comment()).isSameAs(saved.getValue());
    }

    @Test
    void aCommentOnASubtaskCarriesItsKeyAndBelongsToTheStoryOfTheSubtask() {
        adder().execute(new AddCommentCommand(ownerId, workspace.getId(), null, "pru-2", "Done on my side"));

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(saved.capture());
        assertThat(saved.getValue().getSubtaskKey()).isEqualTo("PRU-2");
        assertThat(saved.getValue().getStoryId()).isEqualTo(story.getId());
    }

    @Test
    void someoneOutsideTheWorkspaceCannotComment() {
        assertThatThrownBy(() -> adder().execute(new AddCommentCommand(UUID.randomUUID(), workspace.getId(), "PRU-1", null, "hi")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void aMissingStoryOrSubtaskIsNotFound() {
        assertThatThrownBy(() -> adder().execute(new AddCommentCommand(ownerId, workspace.getId(), "PRU-9", null, "hi")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> adder().execute(new AddCommentCommand(ownerId, workspace.getId(), null, "PRU-9", "hi")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listingGivesEachCommentItsAuthorAndLooksEachAuthorUpOnce() {
        Comment first = Comment.create(workspace.getId(), story.getId(), null, ada.getId(), "one");
        Comment second = Comment.create(workspace.getId(), story.getId(), null, ada.getId(), "two");
        Comment orphan = Comment.create(workspace.getId(), story.getId(), null, null, "from a deleted account");
        when(commentRepository.findOfStory(story.getId())).thenReturn(List.of(first, second, orphan));

        List<CommentView> views = new ListCommentsService(stories, subtasks, commentRepository, authors)
                .execute(new ListCommentsQuery(ownerId, workspace.getId(), "PRU-1", null));

        assertThat(views).extracting(CommentView::authorName).containsExactly("Ada", "Ada", null);
        assertThat(views.get(0).authorAvatarUrl()).isEqualTo("https://img/ada.png");
        verify(userRepository, times(1)).findById(ada.getId());
    }

    @Test
    void listingASubtaskReadsItsOwnCommentsByKey() {
        when(commentRepository.findOfSubtask(workspace.getId(), "PRU-2")).thenReturn(List.of());

        assertThat(new ListCommentsService(stories, subtasks, commentRepository, authors)
                .execute(new ListCommentsQuery(ownerId, workspace.getId(), null, "pru-2"))).isEmpty();

        verify(commentRepository, never()).findOfStory(any());
    }

    @Test
    void theAuthorEditsAndTheCommentIsMarkedAsEdited() {
        Comment comment = existing(memberId, "old");

        CommentView view = new EditCommentService(commentAccess, commentRepository, authors)
                .execute(new EditCommentCommand(memberId, workspace.getId(), comment.getId(), "  new  "));

        assertThat(view.comment().getBody()).isEqualTo("new");
        assertThat(view.comment().getEditedAt()).isNotNull();
        verify(commentRepository).save(comment);
    }

    @Test
    void savingTheSameTextLeavesNoEditedMarkAndWritesNothing() {
        Comment comment = existing(memberId, "same");

        new EditCommentService(commentAccess, commentRepository, authors)
                .execute(new EditCommentCommand(memberId, workspace.getId(), comment.getId(), "same "));

        assertThat(comment.getEditedAt()).isNull();
        verify(commentRepository, never()).save(any());
    }

    @Test
    void nobodyButTheAuthorEditsOrDeletesAComment() {
        Comment comment = existing(memberId, "mine");

        assertThatThrownBy(() -> new EditCommentService(commentAccess, commentRepository, authors)
                .execute(new EditCommentCommand(ownerId, workspace.getId(), comment.getId(), "hijacked")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> new DeleteCommentService(commentAccess, commentRepository)
                .execute(new DeleteCommentCommand(ownerId, workspace.getId(), comment.getId())))
                .isInstanceOf(ForbiddenException.class);

        assertThat(comment.getBody()).isEqualTo("mine");
        verify(commentRepository, never()).save(any());
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void theAuthorDeletesTheComment() {
        Comment comment = existing(memberId, "bye");

        new DeleteCommentService(commentAccess, commentRepository)
                .execute(new DeleteCommentCommand(memberId, workspace.getId(), comment.getId()));

        verify(commentRepository).delete(comment);
    }

    @Test
    void aCommentOfAnotherWorkspaceOrAMissingOneIsNotFound() {
        Comment foreign = Comment.create(UUID.randomUUID(), UUID.randomUUID(), null, memberId, "elsewhere");
        when(commentRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> new DeleteCommentService(commentAccess, commentRepository)
                .execute(new DeleteCommentCommand(memberId, workspace.getId(), foreign.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> new EditCommentService(commentAccess, commentRepository, authors)
                .execute(new EditCommentCommand(memberId, workspace.getId(), UUID.randomUUID(), "x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aCommentWhoseAuthorAccountIsGoneCannotBeChangedByAnyone() {
        Comment orphan = Comment.create(workspace.getId(), story.getId(), null, null, "left behind");
        when(commentRepository.findById(orphan.getId())).thenReturn(Optional.of(orphan));

        assertThatThrownBy(() -> new DeleteCommentService(commentAccess, commentRepository)
                .execute(new DeleteCommentCommand(ownerId, workspace.getId(), orphan.getId())))
                .isInstanceOf(ForbiddenException.class);
    }
}
