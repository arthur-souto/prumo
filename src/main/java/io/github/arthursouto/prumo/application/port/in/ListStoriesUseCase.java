package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListStoriesQuery;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.model.story.UserStory;

public interface ListStoriesUseCase {

    PageResult<UserStory> execute(ListStoriesQuery query);
}
