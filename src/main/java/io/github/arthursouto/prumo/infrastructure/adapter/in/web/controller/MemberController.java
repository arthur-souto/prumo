package io.github.arthursouto.prumo.infrastructure.adapter.in.web.controller;

import io.github.arthursouto.prumo.application.port.in.ListWorkspaceMembersUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListWorkspaceMembersQuery;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.api.MemberApi;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto.MemberResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class MemberController implements MemberApi {

    private final ListWorkspaceMembersUseCase listMembers;

    public MemberController(ListWorkspaceMembersUseCase listMembers) {
        this.listMembers = listMembers;
    }

    @Override
    public List<MemberResponse> list(Authentication authentication, UUID workspaceId) {
        return listMembers.execute(new ListWorkspaceMembersQuery(UUID.fromString(authentication.getName()), workspaceId))
                .stream()
                .map(MemberResponse::from)
                .toList();
    }
}
