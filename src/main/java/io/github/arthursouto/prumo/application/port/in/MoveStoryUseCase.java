package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.MoveStoryCommand;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface MoveStoryUseCase {

    UserStory execute(MoveStoryCommand command);
}
