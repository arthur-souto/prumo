package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.application.port.out.query.StorySearchCriteria;
import io.github.arthursouto.prumo.application.port.out.result.PageResult;
import io.github.arthursouto.prumo.domain.model.story.PullRequest;
import io.github.arthursouto.prumo.domain.model.story.UserStory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoryRepositoryPort {

    UserStory save(UserStory story);

    List<UserStory> saveAll(List<UserStory> stories);

    Optional<UserStory> findById(UUID id);

    Optional<UserStory> findByWorkspaceIdAndKey(UUID workspaceId, String key);

    List<UserStory> findAllByWorkspaceId(UUID workspaceId);

    List<UserStory> findAllByColumnId(UUID columnId);

    List<UserStory> findAllBySprintIds(Collection<UUID> sprintIds);

    PageResult<UserStory> search(StorySearchCriteria criteria);

    List<UserStory> findAllByWorkspaceIdAndKeys(UUID workspaceId, Collection<String> keys);

    long countByWorkspaceId(UUID workspaceId);

    void deleteById(UUID id);

    Optional<PullRequest> findPullRequest(UUID storyId, UUID repositoryId, int number);

    PullRequest savePullRequest(PullRequest pullRequest);

    /** The pull requests tracked on a subtask, in any state. */
    List<PullRequest> findPullRequestsBySubtaskId(UUID subtaskId);

    /** The pull requests tracked on the story itself, the ones that do not belong to a subtask. */
    List<PullRequest> findPullRequestsOfStoryWithoutSubtask(UUID storyId);
}
