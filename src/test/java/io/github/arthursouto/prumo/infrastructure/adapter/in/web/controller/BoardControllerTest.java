package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.CreateColumnUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteColumnUseCase;
import io.github.arthursouto.prumo.application.port.in.GetBoardUseCase;
import io.github.arthursouto.prumo.application.port.in.RenameColumnUseCase;
import io.github.arthursouto.prumo.application.port.in.ReorderColumnsUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateBoardSettingsUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateColumnCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteColumnCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetBoardQuery;
import io.github.arthursouto.prumo.application.port.in.command.RenameColumnCommand;
import io.github.arthursouto.prumo.application.port.in.command.ReorderColumnsCommand;
import io.github.arthursouto.prumo.application.port.in.command.UpdateBoardSettingsCommand;
import io.github.arthursouto.prumo.application.port.in.result.BoardView;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private GetBoardUseCase getBoard;

    @Mock
    private UpdateBoardSettingsUseCase updateSettings;

    @Mock
    private CreateColumnUseCase createColumn;

    @Mock
    private RenameColumnUseCase renameColumn;

    @Mock
    private ReorderColumnsUseCase reorderColumns;

    @Mock
    private DeleteColumnUseCase deleteColumn;

    private MockMvc mockMvc;
    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final String base = "/api/v1/workspaces/" + workspaceId + "/board";
    private Board board;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BoardController(
                        getBoard, updateSettings, createColumn, renameColumn, reorderColumns, deleteColumn
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        board = Board.withDefaults(workspaceId, "PRU");
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(userId.toString(), null);
    }

    @Test
    void getReturnsTheColumnsInOrderWithTheirStories() throws Exception {
        UserStory story = UserStory.create(
                workspaceId, 1, "PRU-1", "T", null, StoryPriority.LOW, null, board.first().getId(), "V",
                List.of(), List.of(), List.of()
        );
        when(getBoard.execute(new GetBoardQuery(userId, workspaceId))).thenReturn(new BoardView(board, List.of(story)));

        mockMvc.perform(get(base).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyPrefix").value("PRU"))
                .andExpect(jsonPath("$.automationEnabled").value(false))
                .andExpect(jsonPath("$.columns[0].name").value("To Do"))
                .andExpect(jsonPath("$.columns[0].kind").value("TODO"))
                .andExpect(jsonPath("$.columns[3].name").value("Done"))
                .andExpect(jsonPath("$.stories[0].key").value("PRU-1"));
    }

    @Test
    void strangersGetNotFound() throws Exception {
        when(getBoard.execute(any())).thenThrow(new ResourceNotFoundException("Workspace not found"));

        mockMvc.perform(get(base).principal(principal())).andExpect(status().isNotFound());
    }

    @Test
    void settingsValidateThePrefixShapeAndReturnTheBoard() throws Exception {
        when(updateSettings.execute(any())).thenReturn(board);
        when(getBoard.execute(any())).thenReturn(new BoardView(board, List.of()));

        mockMvc.perform(put(base + "/settings").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyPrefix\":\"ABC\",\"automationEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.length()").value(4));
        verify(updateSettings).execute(new UpdateBoardSettingsCommand(userId, workspaceId, "ABC", true));

        for (String bad : new String[] {"1AB", "A", "TOOLONGPREFIX", "A-B"}) {
            mockMvc.perform(put(base + "/settings").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"keyPrefix\":\"" + bad + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void settingsErrorsMapTo403And409And422() throws Exception {
        when(updateSettings.execute(any()))
                .thenThrow(new ForbiddenException("no"))
                .thenThrow(new ConflictException("taken"))
                .thenThrow(new BusinessRuleException("locked"));
        String body = "{\"keyPrefix\":\"ABC\"}";

        mockMvc.perform(put(base + "/settings").principal(principal()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(base + "/settings").principal(principal()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(put(base + "/settings").principal(principal()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createsAColumn() throws Exception {
        BoardColumn column = board.addColumn("QA");
        when(createColumn.execute(new CreateColumnCommand(userId, workspaceId, "QA"))).thenReturn(column);

        mockMvc.perform(post(base + "/columns").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"QA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("QA"))
                .andExpect(jsonPath("$.kind").value("CUSTOM"));
    }

    @Test
    void columnNamesAreValidated() throws Exception {
        mockMvc.perform(post(base + "/columns").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(base + "/columns/" + UUID.randomUUID()).principal(principal())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + "x".repeat(61) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createColumn, renameColumn);
    }

    @Test
    void renamesAColumn() throws Exception {
        BoardColumn column = board.first();
        when(renameColumn.execute(new RenameColumnCommand(userId, workspaceId, column.getId(), "Backlog")))
                .thenReturn(column);

        mockMvc.perform(put(base + "/columns/" + column.getId()).principal(principal())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Backlog\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void reordersColumnsThroughALiteralPathThatDoesNotCollideWithTheColumnId() throws Exception {
        List<UUID> ids = board.getColumns().stream().map(BoardColumn::getId).toList();
        when(reorderColumns.execute(new ReorderColumnsCommand(userId, workspaceId, ids))).thenReturn(board);
        when(getBoard.execute(any())).thenReturn(new BoardView(board, List.of()));

        mockMvc.perform(put(base + "/columns/order").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnIds\":[" + ids.stream().map(id -> "\"" + id + "\"")
                                .collect(java.util.stream.Collectors.joining(",")) + "]}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(base + "/columns/order").principal(principal()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columnIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesAColumnPassingTheDestination() throws Exception {
        UUID columnId = UUID.randomUUID();
        UUID destination = UUID.randomUUID();

        mockMvc.perform(delete(base + "/columns/" + columnId).principal(principal()).param("moveTo", destination.toString()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(base + "/columns/" + columnId).principal(principal()))
                .andExpect(status().isNoContent());

        verify(deleteColumn).execute(new DeleteColumnCommand(userId, workspaceId, columnId, destination));
        verify(deleteColumn).execute(new DeleteColumnCommand(userId, workspaceId, columnId, null));
    }

    @Test
    void deletingANonEmptyColumnWithoutADestinationIs422() throws Exception {
        org.mockito.Mockito.doThrow(new BusinessRuleException("Move the stories to another column before removing this one"))
                .when(deleteColumn).execute(any());

        mockMvc.perform(delete(base + "/columns/" + UUID.randomUUID()).principal(principal()))
                .andExpect(status().isUnprocessableEntity());
    }
}
