package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.ChangeSubtaskStatusUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateSubtaskBranchUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.GetSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.ListSubtasksUseCase;
import io.github.arthursouto.prumo.application.port.in.SuggestSubtaskBranchUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ChangeSubtaskStatusCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskBranchCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetSubtaskQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListSubtasksQuery;
import io.github.arthursouto.prumo.application.port.in.command.SuggestSubtaskBranchQuery;
import io.github.arthursouto.prumo.application.port.in.command.UpdateSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.SubtaskApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateBranchRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateSubtaskRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SubtaskResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SubtaskStatusRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UpdateSubtaskRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class SubtaskController implements SubtaskApi {

    private final CreateSubtaskUseCase createSubtask;
    private final ListSubtasksUseCase listSubtasks;
    private final GetSubtaskUseCase getSubtask;
    private final UpdateSubtaskUseCase updateSubtask;
    private final ChangeSubtaskStatusUseCase changeStatus;
    private final DeleteSubtaskUseCase deleteSubtask;
    private final SuggestSubtaskBranchUseCase suggestBranch;
    private final CreateSubtaskBranchUseCase createBranch;

    public SubtaskController(CreateSubtaskUseCase createSubtask,
                             ListSubtasksUseCase listSubtasks,
                             GetSubtaskUseCase getSubtask,
                             UpdateSubtaskUseCase updateSubtask,
                             ChangeSubtaskStatusUseCase changeStatus,
                             DeleteSubtaskUseCase deleteSubtask,
                             SuggestSubtaskBranchUseCase suggestBranch,
                             CreateSubtaskBranchUseCase createBranch) {
        this.createSubtask = createSubtask;
        this.listSubtasks = listSubtasks;
        this.getSubtask = getSubtask;
        this.updateSubtask = updateSubtask;
        this.changeStatus = changeStatus;
        this.deleteSubtask = deleteSubtask;
        this.suggestBranch = suggestBranch;
        this.createBranch = createBranch;
    }

    @Override
    public ResponseEntity<SubtaskResponse> create(Authentication authentication,
                                                  UUID workspaceId,
                                                  String storyKey,
                                                  CreateSubtaskRequest request) {
        SubtaskView view = createSubtask.execute(new CreateSubtaskCommand(
                userId(authentication),
                workspaceId,
                storyKey,
                request.title(),
                request.description(),
                Boolean.TRUE.equals(request.createBranch()),
                request.branchName()
        ));

        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/workspaces/{workspaceId}/subtasks/{key}")
                        .build(workspaceId, view.subtask().getKey()))
                .body(SubtaskResponse.from(view));
    }

    @Override
    public List<SubtaskResponse> list(Authentication authentication, UUID workspaceId, String storyKey) {
        return listSubtasks.execute(new ListSubtasksQuery(userId(authentication), workspaceId, storyKey)).stream()
                .map(SubtaskResponse::from)
                .toList();
    }

    @Override
    public SubtaskResponse get(Authentication authentication, UUID workspaceId, String key) {
        return SubtaskResponse.from(getSubtask.execute(new GetSubtaskQuery(userId(authentication), workspaceId, key)));
    }

    @Override
    public SubtaskResponse update(Authentication authentication, UUID workspaceId, String key, UpdateSubtaskRequest request) {
        return SubtaskResponse.from(updateSubtask.execute(new UpdateSubtaskCommand(
                userId(authentication), workspaceId, key, request.title(), request.description()
        )));
    }

    @Override
    public SubtaskResponse changeStatus(Authentication authentication, UUID workspaceId, String key, SubtaskStatusRequest request) {
        return SubtaskResponse.from(changeStatus.execute(new ChangeSubtaskStatusCommand(
                userId(authentication), workspaceId, key, request.status()
        )));
    }

    @Override
    public ResponseEntity<Void> delete(Authentication authentication, UUID workspaceId, String key) {
        deleteSubtask.execute(new DeleteSubtaskCommand(userId(authentication), workspaceId, key));

        return ResponseEntity.noContent().build();
    }

    @Override
    public Map<String, String> suggestBranch(Authentication authentication, UUID workspaceId, String key) {
        return Map.of("name", suggestBranch.execute(new SuggestSubtaskBranchQuery(userId(authentication), workspaceId, key)));
    }

    @Override
    public SubtaskResponse createBranch(Authentication authentication, UUID workspaceId, String key, CreateBranchRequest request) {
        return SubtaskResponse.from(createBranch.execute(new CreateSubtaskBranchCommand(
                userId(authentication), workspaceId, key, request.name()
        )));
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
