package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.CreateStoryRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.CreateStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.DeleteStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.GetStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.AssignStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.LinkStoryRepositoryUseCase;
import io.github.arthursouto.prumo.application.port.in.ListStoriesUseCase;
import io.github.arthursouto.prumo.application.port.in.MoveStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.SetChecklistItemDoneUseCase;
import io.github.arthursouto.prumo.application.port.in.UpdateStoryUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ChecklistInput;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.CreateStoryRepositoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.DeleteStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.GetStoryQuery;
import io.github.arthursouto.prumo.application.port.in.command.AssignStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.LinkStoryRepositoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.ListStoriesQuery;
import io.github.arthursouto.prumo.application.port.in.command.MoveStoryCommand;
import io.github.arthursouto.prumo.application.port.in.command.SetChecklistItemDoneCommand;
import io.github.arthursouto.prumo.application.port.in.command.UpdateStoryCommand;
import io.github.arthursouto.prumo.domain.model.story.StoryPriority;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.StoryApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ChecklistItemRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateStoryRepositoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.CreateStoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.AssignStoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.LinkRepositoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.MoveStoryRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.SetChecklistDoneRequest;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.StoryResponse;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.UpdateStoryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class StoryController implements StoryApi {

    private final CreateStoryUseCase createStory;
    private final ListStoriesUseCase listStories;
    private final GetStoryUseCase getStory;
    private final UpdateStoryUseCase updateStory;
    private final DeleteStoryUseCase deleteStory;
    private final SetChecklistItemDoneUseCase setChecklistItemDone;
    private final MoveStoryUseCase moveStory;
    private final LinkStoryRepositoryUseCase linkRepository;
    private final CreateStoryRepositoryUseCase createRepository;
    private final AssignStoryUseCase assignStory;

    public StoryController(CreateStoryUseCase createStory,
                           ListStoriesUseCase listStories,
                           GetStoryUseCase getStory,
                           UpdateStoryUseCase updateStory,
                           DeleteStoryUseCase deleteStory,
                           SetChecklistItemDoneUseCase setChecklistItemDone,
                           MoveStoryUseCase moveStory,
                           LinkStoryRepositoryUseCase linkRepository,
                           CreateStoryRepositoryUseCase createRepository,
                           AssignStoryUseCase assignStory) {
        this.createStory = createStory;
        this.listStories = listStories;
        this.getStory = getStory;
        this.updateStory = updateStory;
        this.deleteStory = deleteStory;
        this.setChecklistItemDone = setChecklistItemDone;
        this.moveStory = moveStory;
        this.linkRepository = linkRepository;
        this.createRepository = createRepository;
        this.assignStory = assignStory;
    }

    @Override
    public ResponseEntity<StoryResponse> create(Authentication authentication,
                                                UUID workspaceId,
                                                CreateStoryRequest request) {
        UserStory story = createStory.execute(new CreateStoryCommand(
                userId(authentication),
                workspaceId,
                request.title(),
                request.description(),
                request.priority(),
                request.storyPoints(),
                request.labels(),
                inputs(request.acceptanceCriteria()),
                request.columnId()
        ));

        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{key}").build(story.getKey()))
                .body(StoryResponse.from(story));
    }

    @Override
    public List<StoryResponse> list(Authentication authentication,
                                    UUID workspaceId,
                                    String label,
                                    StoryPriority priority,
                                    String text) {
        return listStories.execute(new ListStoriesQuery(userId(authentication), workspaceId, label, priority, text))
                .stream()
                .map(StoryResponse::from)
                .toList();
    }

    @Override
    public StoryResponse get(Authentication authentication, UUID workspaceId, String key) {
        return StoryResponse.from(getStory.execute(new GetStoryQuery(userId(authentication), workspaceId, key)));
    }

    @Override
    public StoryResponse update(Authentication authentication,
                                UUID workspaceId,
                                String key,
                                UpdateStoryRequest request) {
        return StoryResponse.from(updateStory.execute(new UpdateStoryCommand(
                userId(authentication),
                workspaceId,
                key,
                request.title(),
                request.description(),
                request.priority(),
                request.storyPoints(),
                request.labels(),
                inputs(request.acceptanceCriteria())
        )));
    }

    @Override
    public ResponseEntity<Void> delete(Authentication authentication, UUID workspaceId, String key) {
        deleteStory.execute(new DeleteStoryCommand(userId(authentication), workspaceId, key));

        return ResponseEntity.noContent().build();
    }

    @Override
    public StoryResponse setChecklistItemDone(Authentication authentication,
                                              UUID workspaceId,
                                              String key,
                                              UUID itemId,
                                              SetChecklistDoneRequest request) {
        return StoryResponse.from(setChecklistItemDone.execute(new SetChecklistItemDoneCommand(
                userId(authentication),
                workspaceId,
                key,
                itemId,
                request.done()
        )));
    }

    @Override
    public StoryResponse move(Authentication authentication, UUID workspaceId, String key, MoveStoryRequest request) {
        return StoryResponse.from(moveStory.execute(new MoveStoryCommand(
                userId(authentication),
                workspaceId,
                key,
                request.columnId(),
                request.afterStoryId(),
                request.beforeStoryId()
        )));
    }

    @Override
    public StoryResponse linkRepository(Authentication authentication,
                                        UUID workspaceId,
                                        String key,
                                        LinkRepositoryRequest request) {
        return StoryResponse.from(linkRepository.execute(new LinkStoryRepositoryCommand(
                userId(authentication),
                workspaceId,
                key,
                request.repositoryId()
        )));
    }

    @Override
    public StoryResponse assign(Authentication authentication, UUID workspaceId, String key, AssignStoryRequest request) {
        return StoryResponse.from(assignStory.execute(new AssignStoryCommand(
                userId(authentication),
                workspaceId,
                key,
                request.assigneeId()
        )));
    }

    @Override
    public StoryResponse createRepository(Authentication authentication,
                                          UUID workspaceId,
                                          String key,
                                          CreateStoryRepositoryRequest request) {
        return StoryResponse.from(createRepository.execute(new CreateStoryRepositoryCommand(
                userId(authentication),
                workspaceId,
                key,
                request.name(),
                request.description(),
                Boolean.TRUE.equals(request.isPrivate())
        )));
    }

    private static List<ChecklistInput> inputs(List<ChecklistItemRequest> items) {
        return items == null
                ? List.of()
                : items.stream().map(item -> new ChecklistInput(item.id(), item.text(), Boolean.TRUE.equals(item.done()))).toList();
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
