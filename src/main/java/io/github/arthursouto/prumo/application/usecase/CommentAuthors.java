package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.result.CommentView;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.model.comment.Comment;
import io.github.arthursouto.prumo.domain.model.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Puts the author's name and picture on comments, looking each author up once. */
public class CommentAuthors {

    private final UserRepositoryPort userRepository;

    public CommentAuthors(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    public CommentView view(Comment comment) {
        return views(List.of(comment)).get(0);
    }

    public List<CommentView> views(List<Comment> comments) {
        Map<UUID, Optional<User>> authors = new HashMap<>();

        return comments.stream()
                .map(comment -> {
                    Optional<User> author = comment.getAuthorId() == null
                            ? Optional.empty()
                            : authors.computeIfAbsent(comment.getAuthorId(), userRepository::findById);

                    return new CommentView(comment, author.map(User::getName).orElse(null), author.map(User::getAvatarUrl).orElse(null));
                })
                .toList();
    }
}
