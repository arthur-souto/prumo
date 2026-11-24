package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListStoriesQuery;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import java.util.List;

public interface ListStoriesUseCase {

    List<UserStory> execute(ListStoriesQuery command);
}
