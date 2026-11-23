package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetCurrentUserUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetCurrentUserQuery;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.user.User;

public class GetCurrentUserService implements GetCurrentUserUseCase {

    private final UserRepositoryPort userRepository;

    public GetCurrentUserService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User execute(GetCurrentUserQuery query) {
        return userRepository.findById(query.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
