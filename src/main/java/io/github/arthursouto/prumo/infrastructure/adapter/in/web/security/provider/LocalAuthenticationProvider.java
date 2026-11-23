package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.provider;

import io.github.arthursouto.prumo.application.exceptions.InvalidCredentialsException;
import io.github.arthursouto.prumo.application.port.in.AuthenticateUserUseCase;
import io.github.arthursouto.prumo.application.port.in.command.AuthenticateUserCommand;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.auth.AuthenticatedUser;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class LocalAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticateUserUseCase authenticateUser;

    public LocalAuthenticationProvider(AuthenticateUserUseCase authenticateUser) {
        this.authenticateUser = authenticateUser;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        try {
            User user = authenticateUser.execute(new AuthenticateUserCommand(
                    authentication.getName(),
                    String.valueOf(authentication.getCredentials())
            ));

            return UsernamePasswordAuthenticationToken.authenticated(
                    AuthenticatedUser.from(user),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        } catch (InvalidCredentialsException exception) {
            throw new BadCredentialsException(exception.getMessage(), exception);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
