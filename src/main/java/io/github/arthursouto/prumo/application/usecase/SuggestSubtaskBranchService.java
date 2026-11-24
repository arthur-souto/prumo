package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.SuggestSubtaskBranchUseCase;
import io.github.arthursouto.prumo.application.port.in.command.SuggestSubtaskBranchQuery;
import io.github.arthursouto.prumo.domain.model.github.BranchNames;
import io.github.arthursouto.prumo.domain.model.story.Subtask;

public class SuggestSubtaskBranchService implements SuggestSubtaskBranchUseCase {

    private final SubtaskLookup lookup;

    public SuggestSubtaskBranchService(SubtaskLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public String execute(SuggestSubtaskBranchQuery query) {
        Subtask subtask = lookup.asMember(query.userId(), query.workspaceId(), query.key());

        return BranchNames.suggest(subtask.getKey(), subtask.getTitle());
    }
}
