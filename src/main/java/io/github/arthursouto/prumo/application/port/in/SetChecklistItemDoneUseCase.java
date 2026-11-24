package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.SetChecklistItemDoneCommand;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface SetChecklistItemDoneUseCase {

    UserStory execute(SetChecklistItemDoneCommand command);
}
