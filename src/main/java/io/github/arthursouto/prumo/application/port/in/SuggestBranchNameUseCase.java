package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.SuggestBranchNameQuery;

public interface SuggestBranchNameUseCase {

    String execute(SuggestBranchNameQuery command);
}
