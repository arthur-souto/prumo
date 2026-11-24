package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.UpdateSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.command.UpdateSubtaskCommand;
import io.github.arthursouto.prumo.application.port.in.result.SubtaskView;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;
import io.github.arthursouto.prumo.domain.model.story.Subtask;

public class UpdateSubtaskService implements UpdateSubtaskUseCase {

    private final SubtaskLookup lookup;
    private final SubtaskRepositoryPort subtaskRepository;

    public UpdateSubtaskService(SubtaskLookup lookup, SubtaskRepositoryPort subtaskRepository) {
        this.lookup = lookup;
        this.subtaskRepository = subtaskRepository;
    }

    @Override
    public SubtaskView execute(UpdateSubtaskCommand command) {
        Subtask subtask = lookup.asMember(command.userId(), command.workspaceId(), command.key());

        subtask.update(command.title().strip(), Checklists.text(command.description()));

        return lookup.view(subtaskRepository.save(subtask));
    }
}
