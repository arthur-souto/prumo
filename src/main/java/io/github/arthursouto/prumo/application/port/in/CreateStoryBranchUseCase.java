package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.CreateStoryBranchCommand;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface CreateStoryBranchUseCase {

    UserStory execute(CreateStoryBranchCommand command);
}
