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
import io.github.arthursouto.prumo.domain.model.board.BoardColumn;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.BoardApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.BoardResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ColumnNameRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ReorderColumnsRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UpdateBoardSettingsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class BoardController implements BoardApi {

    private final GetBoardUseCase getBoard;
    private final UpdateBoardSettingsUseCase updateSettings;
    private final CreateColumnUseCase createColumn;
    private final RenameColumnUseCase renameColumn;
    private final ReorderColumnsUseCase reorderColumns;
    private final DeleteColumnUseCase deleteColumn;

    public BoardController(GetBoardUseCase getBoard,
                           UpdateBoardSettingsUseCase updateSettings,
                           CreateColumnUseCase createColumn,
                           RenameColumnUseCase renameColumn,
                           ReorderColumnsUseCase reorderColumns,
                           DeleteColumnUseCase deleteColumn) {
        this.getBoard = getBoard;
        this.updateSettings = updateSettings;
        this.createColumn = createColumn;
        this.renameColumn = renameColumn;
        this.reorderColumns = reorderColumns;
        this.deleteColumn = deleteColumn;
    }

    @Override
    public BoardResponse get(Authentication authentication, UUID workspaceId) {
        return full(authentication, workspaceId);
    }

    @Override
    public BoardResponse updateSettings(Authentication authentication,
                                        UUID workspaceId,
                                        UpdateBoardSettingsRequest request) {
        updateSettings.execute(new UpdateBoardSettingsCommand(
                userId(authentication),
                workspaceId,
                request.keyPrefix(),
                request.automationEnabled(),
                request.consolidateStories()
        ));

        return full(authentication, workspaceId);
    }

    @Override
    public ResponseEntity<BoardResponse.ColumnResponse> createColumn(Authentication authentication,
                                                                     UUID workspaceId,
                                                                     ColumnNameRequest request) {
        BoardColumn column = createColumn.execute(new CreateColumnCommand(
                userId(authentication),
                workspaceId,
                request.name()
        ));

        return ResponseEntity.status(201).body(BoardResponse.ColumnResponse.from(column));
    }

    @Override
    public BoardResponse.ColumnResponse renameColumn(Authentication authentication,
                                                     UUID workspaceId,
                                                     UUID columnId,
                                                     ColumnNameRequest request) {
        return BoardResponse.ColumnResponse.from(renameColumn.execute(new RenameColumnCommand(
                userId(authentication),
                workspaceId,
                columnId,
                request.name()
        )));
    }

    @Override
    public BoardResponse reorderColumns(Authentication authentication,
                                        UUID workspaceId,
                                        ReorderColumnsRequest request) {
        reorderColumns.execute(new ReorderColumnsCommand(userId(authentication), workspaceId, request.columnIds()));

        return full(authentication, workspaceId);
    }

    @Override
    public ResponseEntity<Void> deleteColumn(Authentication authentication,
                                             UUID workspaceId,
                                             UUID columnId,
                                             UUID moveTo) {
        deleteColumn.execute(new DeleteColumnCommand(userId(authentication), workspaceId, columnId, moveTo));

        return ResponseEntity.noContent().build();
    }

    private BoardResponse full(Authentication authentication, UUID workspaceId) {
        BoardView view = getBoard.execute(new GetBoardQuery(userId(authentication), workspaceId));

        return BoardResponse.from(view.board(), view.stories(), view.subtaskProgress());
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
