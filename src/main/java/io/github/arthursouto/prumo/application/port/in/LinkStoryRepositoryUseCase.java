package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.LinkStoryRepositoryCommand;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface LinkStoryRepositoryUseCase {

    UserStory execute(LinkStoryRepositoryCommand command);
}
