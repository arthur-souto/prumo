package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListWorkspaceMembersQuery;
import io.github.arthursouto.prumo.application.port.in.result.MemberView;

import java.util.List;

public interface ListWorkspaceMembersUseCase {

    List<MemberView> execute(ListWorkspaceMembersQuery query);
}
