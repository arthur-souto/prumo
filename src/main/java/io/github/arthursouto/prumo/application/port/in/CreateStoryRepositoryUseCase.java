package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.CreateStoryRepositoryCommand;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface CreateStoryRepositoryUseCase {

    UserStory execute(CreateStoryRepositoryCommand command);
}
