package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.CommentRepositoryPort;
import io.github.arthursouto.prumo.domain.model.comment.Comment;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.CommentEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.CommentJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CommentPersistenceAdapter implements CommentRepositoryPort {

    private final CommentJpaRepository repository;

    public CommentPersistenceAdapter(CommentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Comment save(Comment comment) {
        return toDomain(repository.saveAndFlush(toEntity(comment)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comment> findById(UUID id) {
        return repository.findById(id).map(CommentPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public void delete(Comment comment) {
        repository.deleteById(comment.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findOfStory(UUID storyId) {
        return repository.findByStoryIdAndSubtaskKeyIsNullOrderByCreatedAtAsc(storyId).stream()
                .map(CommentPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findOfSubtask(UUID workspaceId, String subtaskKey) {
        return repository.findByWorkspaceIdAndSubtaskKeyOrderByCreatedAtAsc(workspaceId, subtaskKey).stream()
                .map(CommentPersistenceAdapter::toDomain)
                .toList();
    }

    private static CommentEntity toEntity(Comment comment) {
        CommentEntity entity = new CommentEntity();
        entity.setId(comment.getId());
        entity.setWorkspaceId(comment.getWorkspaceId());
        entity.setStoryId(comment.getStoryId());
        entity.setSubtaskKey(comment.getSubtaskKey());
        entity.setAuthorId(comment.getAuthorId());
        entity.setBody(comment.getBody());
        entity.setCreatedAt(comment.getCreatedAt());
        entity.setEditedAt(comment.getEditedAt());
        return entity;
    }

    private static Comment toDomain(CommentEntity entity) {
        return Comment.restore(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getStoryId(),
                entity.getSubtaskKey(),
                entity.getAuthorId(),
                entity.getBody(),
                entity.getCreatedAt(),
                entity.getEditedAt()
        );
    }
}
