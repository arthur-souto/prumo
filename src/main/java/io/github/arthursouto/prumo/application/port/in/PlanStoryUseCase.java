package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.PlanStoryCommand;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface PlanStoryUseCase {

    UserStory execute(PlanStoryCommand command);
}
