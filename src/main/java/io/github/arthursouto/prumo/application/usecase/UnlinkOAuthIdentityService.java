package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.UnlinkOAuthIdentityUseCase;
import io.github.arthursouto.prumo.application.port.in.command.UnlinkOAuthIdentityCommand;
import io.github.arthursouto.prumo.application.port.out.UserIdentityRepositoryPort;
import io.github.arthursouto.prumo.application.port.out.UserRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;

import java.util.List;

public class UnlinkOAuthIdentityService implements UnlinkOAuthIdentityUseCase {

    private final UserRepositoryPort userRepository;
    private final UserIdentityRepositoryPort identityRepository;

    public UnlinkOAuthIdentityService(UserRepositoryPort userRepository, UserIdentityRepositoryPort identityRepository) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
    }

    @Override
    public void execute(UnlinkOAuthIdentityCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<UserIdentity> identities = identityRepository.findByUserId(user.getId());

        UserIdentity target = identities.stream()
                .filter(identity -> identity.getProvider() == command.provider())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Identity not linked"));

        if (!user.hasPassword() && identities.size() <= 1) {
            throw new BusinessRuleException("Set a password before unlinking the only login method");
        }

        identityRepository.delete(target.getId());
    }
}
