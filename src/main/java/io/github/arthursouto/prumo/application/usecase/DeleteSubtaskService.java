package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.DeleteSubtaskUseCase;
import io.github.arthursouto.prumo.application.port.in.command.DeleteSubtaskCommand;
import io.github.arthursouto.prumo.application.port.out.SubtaskRepositoryPort;

public class DeleteSubtaskService implements DeleteSubtaskUseCase {

    private final SubtaskLookup lookup;
    private final SubtaskRepositoryPort subtaskRepository;

    public DeleteSubtaskService(SubtaskLookup lookup, SubtaskRepositoryPort subtaskRepository) {
        this.lookup = lookup;
        this.subtaskRepository = subtaskRepository;
    }

    @Override
    public void execute(DeleteSubtaskCommand command) {
        subtaskRepository.deleteById(lookup.asMember(command.userId(), command.workspaceId(), command.key()).getId());
    }
}
