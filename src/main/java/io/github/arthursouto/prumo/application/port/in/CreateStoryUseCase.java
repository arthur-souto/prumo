package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.CreateStoryCommand;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface CreateStoryUseCase {

    UserStory execute(CreateStoryCommand command);
}
