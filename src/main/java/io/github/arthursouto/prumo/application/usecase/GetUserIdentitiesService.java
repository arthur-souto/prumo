package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.GetUserIdentitiesUseCase;
import io.github.arthursouto.prumo.application.port.in.command.GetUserIdentitiesQuery;
import io.github.arthursouto.prumo.application.port.out.UserIdentityRepositoryPort;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;

import java.util.List;

public class GetUserIdentitiesService implements GetUserIdentitiesUseCase {

    private final UserIdentityRepositoryPort identityRepository;

    public GetUserIdentitiesService(UserIdentityRepositoryPort identityRepository) {
        this.identityRepository = identityRepository;
    }

    @Override
    public List<UserIdentity> execute(GetUserIdentitiesQuery query) {
        return identityRepository.findByUserId(query.userId());
    }
}
