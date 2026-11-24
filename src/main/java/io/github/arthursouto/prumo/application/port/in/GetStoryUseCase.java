package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetStoryQuery;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface GetStoryUseCase {

    UserStory execute(GetStoryQuery command);
}
