package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ChangeSubtaskStatusUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ChangeSubtaskStatusCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.domain.model.board.Board;
import io.github.arthursouto.prumo.domain.model.story.Subtask;

public class ChangeSubtaskStatusService implements ChangeSubtaskStatusUseCase {

    private final SubtaskLookup lookup;
    private final BoardProvisioner boards;
    private final SubtaskWorkflow workflow;

    public ChangeSubtaskStatusService(SubtaskLookup lookup, BoardProvisioner boards, SubtaskWorkflow workflow) {
        this.lookup = lookup;
        this.boards = boards;
        this.workflow = workflow;
    }

    @Override
    public SubtaskView execute(ChangeSubtaskStatusCommand command) {
        Subtask subtask = lookup.asMember(command.userId(), command.workspaceId(), command.key());
        Board board = boards.ensure(lookup.workspace(command.userId(), command.workspaceId()));

        return lookup.view(workflow.changeStatus(board, subtask, command.status(), Actor.user(command.userId())));
    }
}
