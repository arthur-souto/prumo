package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.CloseSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.GetSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.ListSprintsUseCase;
import io.github.arthursouto.prumo.application.port.in.PlanStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.StartSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateSprintUseCase;
import io.github.arthursouto.prumo.application.port.in.command.CreateSprintCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetSprintQuery;
import io.github.arthursouto.prumo.application.port.in.command.ListSprintsQuery;
import io.github.arthursouto.prumo.application.port.in.command.PlanStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.SprintCommand;
import io.github.arthursouto.prumo.application.port.in.command.UpdateSprintCommand;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.SprintApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.PageResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.PlanStoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SprintRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SprintResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SprintSearchRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.StoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
public class SprintController implements SprintApi {

    private final ListSprintsUseCase listSprints;
    private final GetSprintUseCase getSprint;
    private final CreateSprintUseCase createSprint;
    private final UpdateSprintUseCase updateSprint;
    private final StartSprintUseCase startSprint;
    private final CloseSprintUseCase closeSprint;
    private final DeleteSprintUseCase deleteSprint;
    private final PlanStoryUseCase planStory;

    public SprintController(ListSprintsUseCase listSprints,
                            GetSprintUseCase getSprint,
                            CreateSprintUseCase createSprint,
                            UpdateSprintUseCase updateSprint,
                            StartSprintUseCase startSprint,
                            CloseSprintUseCase closeSprint,
                            DeleteSprintUseCase deleteSprint,
                            PlanStoryUseCase planStory) {
        this.listSprints = listSprints;
        this.getSprint = getSprint;
        this.createSprint = createSprint;
        this.updateSprint = updateSprint;
        this.startSprint = startSprint;
        this.closeSprint = closeSprint;
        this.deleteSprint = deleteSprint;
        this.planStory = planStory;
    }

    @Override
    public PageResponse<SprintResponse> list(Authentication authentication, UUID workspaceId, SprintSearchRequest search) {
        return PageResponse.from(listSprints.execute(new ListSprintsQuery(
                userId(authentication),
                workspaceId,
                search.q(),
                search.state(),
                search.page(),
                search.size()
        )), SprintResponse::from);
    }

    @Override
    public SprintResponse get(Authentication authentication, UUID workspaceId, UUID sprintId) {
        return SprintResponse.from(getSprint.execute(new GetSprintQuery(userId(authentication), workspaceId, sprintId)));
    }

    @Override
    public ResponseEntity<SprintResponse> create(Authentication authentication, UUID workspaceId, SprintRequest request) {
        SprintResponse created = SprintResponse.from(createSprint.execute(new CreateSprintCommand(
                userId(authentication),
                workspaceId,
                request.name(),
                request.goal(),
                request.startDate(),
                request.endDate()
        )));

        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(created.id()))
                .body(created);
    }

    @Override
    public SprintResponse update(Authentication authentication, UUID workspaceId, UUID sprintId, SprintRequest request) {
        return SprintResponse.from(updateSprint.execute(new UpdateSprintCommand(
                userId(authentication),
                workspaceId,
                sprintId,
                request.name(),
                request.goal(),
                request.startDate(),
                request.endDate()
        )));
    }

    @Override
    public SprintResponse start(Authentication authentication, UUID workspaceId, UUID sprintId) {
        return SprintResponse.from(startSprint.execute(new SprintCommand(userId(authentication), workspaceId, sprintId)));
    }

    @Override
    public SprintResponse close(Authentication authentication, UUID workspaceId, UUID sprintId) {
        return SprintResponse.from(closeSprint.execute(new SprintCommand(userId(authentication), workspaceId, sprintId)));
    }

    @Override
    public ResponseEntity<Void> delete(Authentication authentication, UUID workspaceId, UUID sprintId) {
        deleteSprint.execute(new SprintCommand(userId(authentication), workspaceId, sprintId));

        return ResponseEntity.noContent().build();
    }

    @Override
    public StoryResponse plan(Authentication authentication, UUID workspaceId, String key, PlanStoryRequest request) {
        return StoryResponse.from(planStory.execute(new PlanStoryCommand(userId(authentication), workspaceId, key, request.sprintId())));
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
