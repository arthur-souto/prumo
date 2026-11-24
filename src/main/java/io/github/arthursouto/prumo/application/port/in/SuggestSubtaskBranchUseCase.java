package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.SuggestSubtaskBranchQuery;

public interface SuggestSubtaskBranchUseCase {

    String execute(SuggestSubtaskBranchQuery query);
}
