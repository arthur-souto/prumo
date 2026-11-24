package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.AssignStoryCommand;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface AssignStoryUseCase {

    UserStory execute(AssignStoryCommand command);
}
