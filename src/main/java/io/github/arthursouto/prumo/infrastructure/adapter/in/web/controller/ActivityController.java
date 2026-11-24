package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.ListActivityUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListActivityQuery;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.ActivityApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.ActivityResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ActivityController implements ActivityApi {

    private final ListActivityUseCase listActivity;

    public ActivityController(ListActivityUseCase listActivity) {
        this.listActivity = listActivity;
    }

    @Override
    public List<ActivityResponse> ofStory(Authentication authentication, UUID workspaceId, String key) {
        return listActivity.execute(ListActivityQuery.ofStory(userId(authentication), workspaceId, key)).stream()
                .map(ActivityResponse::from)
                .toList();
    }

    @Override
    public List<ActivityResponse> ofSubtask(Authentication authentication, UUID workspaceId, String key) {
        return listActivity.execute(ListActivityQuery.ofSubtask(userId(authentication), workspaceId, key)).stream()
                .map(ActivityResponse::from)
                .toList();
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
