package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.story.Subtask;
import io.github.arthursouto.prumo.domain.model.story.SubtaskProgress;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SubtaskRepositoryPort {

    Subtask save(Subtask subtask);

    Optional<Subtask> findByWorkspaceIdAndKey(UUID workspaceId, String key);

    List<Subtask> findAllByStoryId(UUID storyId);

    List<Subtask> findAllByWorkspaceIdAndKeys(UUID workspaceId, Collection<String> keys);

    Map<UUID, SubtaskProgress> progressByStoryIds(Collection<UUID> storyIds);

    void deleteById(UUID id);
}
