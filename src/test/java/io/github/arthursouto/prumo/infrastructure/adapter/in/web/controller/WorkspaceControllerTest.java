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
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;
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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkspaceControllerTest {

    @Mock
    private CreateWorkspaceUseCase createWorkspace;

    @Mock
    private ListWorkspacesUseCase listWorkspaces;

    @Mock
    private GetWorkspaceUseCase getWorkspace;

    @Mock
    private UpdateWorkspaceUseCase updateWorkspace;

    @Mock
    private DeleteWorkspaceUseCase deleteWorkspace;

    private MockMvc mockMvc;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WorkspaceController(
                        createWorkspace,
                        listWorkspaces,
                        getWorkspace,
                        updateWorkspace,
                        deleteWorkspace
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturnsCreatedWithLocationAndTheOwnerRole() throws Exception {
        Workspace workspace = Workspace.create("Acme", "acme", "Desc", userId);
        when(createWorkspace.execute(new CreateWorkspaceCommand(userId, "Acme", "Desc"))).thenReturn(workspace);

        mockMvc.perform(post("/api/v1/workspaces")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme","description":"Desc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/workspaces/" + workspace.getId()))
                .andExpect(jsonPath("$.id").value(workspace.getId().toString()))
                .andExpect(jsonPath("$.slug").value("acme"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.ownerId").doesNotExist());
    }

    @Test
    void createRejectsBlankAndTooLongFields() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","description":"%s"}
                                """.formatted("x".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));

        verifyNoInteractions(createWorkspace);
    }

    @Test
    void listReturnsTheWorkspacesOfTheCaller() throws Exception {
        when(listWorkspaces.execute(new ListWorkspacesQuery(userId)))
                .thenReturn(List.of(Workspace.create("Acme", "acme", null, userId)));

        mockMvc.perform(get("/api/v1/workspaces").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Acme"));
    }

    @Test
    void getReturnsTheWorkspaceOrNotFound() throws Exception {
        Workspace workspace = Workspace.create("Acme", "acme", null, userId);
        UUID missing = UUID.randomUUID();
        when(getWorkspace.execute(new GetWorkspaceQuery(userId, workspace.getId()))).thenReturn(workspace);
        when(getWorkspace.execute(new GetWorkspaceQuery(userId, missing)))
                .thenThrow(new ResourceNotFoundException("Workspace not found"));

        mockMvc.perform(get("/api/v1/workspaces/" + workspace.getId()).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("acme"));
        mockMvc.perform(get("/api/v1/workspaces/" + missing).principal(principal()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsTheUpdatedWorkspace() throws Exception {
        Workspace workspace = Workspace.create("New", "acme", null, userId);
        when(updateWorkspace.execute(new UpdateWorkspaceCommand(userId, workspace.getId(), "New", null)))
                .thenReturn(workspace);

        mockMvc.perform(put("/api/v1/workspaces/" + workspace.getId())
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    void updateIsForbiddenForPlainMembersAndValidatesTheBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateWorkspace.execute(new UpdateWorkspaceCommand(userId, id, "x", null)))
                .thenThrow(new ForbiddenException("Only owners and admins can edit a workspace"));

        mockMvc.perform(put("/api/v1/workspaces/" + id)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"x"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/workspaces/" + id)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/workspaces/" + id).principal(principal()))
                .andExpect(status().isNoContent());

        verify(deleteWorkspace).execute(new DeleteWorkspaceCommand(userId, id));
    }

    @Test
    void deleteIsForbiddenForNonOwners() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ForbiddenException("Only the owner can delete a workspace"))
                .when(deleteWorkspace).execute(new DeleteWorkspaceCommand(userId, id));

        mockMvc.perform(delete("/api/v1/workspaces/" + id).principal(principal()))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken principal() {
        return UsernamePasswordAuthenticationToken.authenticated(userId.toString(), null, List.of());
    }
}
