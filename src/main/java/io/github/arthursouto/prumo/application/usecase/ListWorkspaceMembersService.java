package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.ListWorkspaceMembersUseCase;
import io.github.arthursouto.prumo.application.port.in.command.ListWorkspaceMembersQuery;
import io.github.arthursouto.prumo.application.port.in.result.MemberView;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;

import java.util.List;

public class ListWorkspaceMembersService implements ListWorkspaceMembersUseCase {

    private final WorkspaceAccess access;
    private final UserRepositoryPort userRepository;

    public ListWorkspaceMembersService(WorkspaceAccess access, UserRepositoryPort userRepository) {
        this.access = access;
        this.userRepository = userRepository;
    }

    @Override
    public List<MemberView> execute(ListWorkspaceMembersQuery query) {
        return access.member(query.userId(), query.workspaceId()).getMembers().stream()
                .flatMap(member -> userRepository.findById(member.getUserId()).stream()
                        .map(user -> new MemberView(user.getId(), user.getName(), user.getAvatarUrl(), member.getRole())))
                .toList();
    }
}
