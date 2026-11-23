package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.CreateWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.GetWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.ListWorkspacesUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateWorkspaceUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateWorkspaceCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteWorkspaceCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetWorkspaceQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListWorkspacesQuery;
import io.github.arthursouto.prumo.application.port.in.command.UpdateWorkspaceCommand;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.WorkspaceApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateWorkspaceRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UpdateWorkspaceRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.WorkspaceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
public class WorkspaceController implements WorkspaceApi {

    private final CreateWorkspaceUseCase createWorkspace;
    private final ListWorkspacesUseCase listWorkspaces;
    private final GetWorkspaceUseCase getWorkspace;
    private final UpdateWorkspaceUseCase updateWorkspace;
    private final DeleteWorkspaceUseCase deleteWorkspace;

    public WorkspaceController(CreateWorkspaceUseCase createWorkspace,
                               ListWorkspacesUseCase listWorkspaces,
                               GetWorkspaceUseCase getWorkspace,
                               UpdateWorkspaceUseCase updateWorkspace,
                               DeleteWorkspaceUseCase deleteWorkspace) {
        this.createWorkspace = createWorkspace;
        this.listWorkspaces = listWorkspaces;
        this.getWorkspace = getWorkspace;
        this.updateWorkspace = updateWorkspace;
        this.deleteWorkspace = deleteWorkspace;
    }

    @Override
    public ResponseEntity<WorkspaceResponse> create(Authentication authentication, CreateWorkspaceRequest request) {
        UUID userId = userId(authentication);
        Workspace workspace = createWorkspace.execute(new CreateWorkspaceCommand(
                userId,
                request.name(),
                request.description()
        ));

        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(workspace.getId()))
                .body(WorkspaceResponse.from(workspace, userId));
    }

    @Override
    public List<WorkspaceResponse> list(Authentication authentication) {
        UUID userId = userId(authentication);

        return listWorkspaces.execute(new ListWorkspacesQuery(userId)).stream()
                .map(workspace -> WorkspaceResponse.from(workspace, userId))
                .toList();
    }

    @Override
    public WorkspaceResponse get(Authentication authentication, UUID id) {
        UUID userId = userId(authentication);

        return WorkspaceResponse.from(getWorkspace.execute(new GetWorkspaceQuery(userId, id)), userId);
    }

    @Override
    public WorkspaceResponse update(Authentication authentication, UUID id, UpdateWorkspaceRequest request) {
        UUID userId = userId(authentication);

        return WorkspaceResponse.from(
                updateWorkspace.execute(new UpdateWorkspaceCommand(userId, id, request.name(), request.description())),
                userId
        );
    }

    @Override
    public ResponseEntity<Void> delete(Authentication authentication, UUID id) {
        deleteWorkspace.execute(new DeleteWorkspaceCommand(userId(authentication), id));

        return ResponseEntity.noContent().build();
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
