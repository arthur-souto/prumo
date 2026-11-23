package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security;

import io.github.arthursouto.prumo.application.exceptions.InvalidCredentialsException;
import io.github.arthursouto.prumo.application.port.in.AuthenticateUserUseCase;
import io.github.arthursouto.prumo.application.port.in.command.AuthenticateUserCommand;
import io.github.arthursouto.prumo.domain.model.user.Role;
import io.github.arthursouto.prumo.domain.model.user.User;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.auth.AuthenticatedUser;
import io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.provider.LocalAuthenticationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalAuthenticationProviderTest {

    @Mock
    private AuthenticateUserUseCase authenticateUser;

    private LocalAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalAuthenticationProvider(authenticateUser);
    }

    @Test
    void returnsAuthenticatedTokenWithUserAndRoleAuthority() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        AuthenticateUserCommand command = new AuthenticateUserCommand("arthur@prumo.io", "s3cret-pass");
        when(authenticateUser.execute(command)).thenReturn(user);

        Authentication result = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("arthur@prumo.io", "s3cret-pass")
        );

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getCredentials()).isNull();
        assertThat(result.getPrincipal()).isEqualTo(AuthenticatedUser.from(user));
        assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
        verify(authenticateUser).execute(command);
    }

    @Test
    void exposesAdminAuthorityForAdminUsers() {
        User user = User.registerLocal("Arthur", "arthur@prumo.io", "hashed");
        user.changeRole(Role.ADMIN);
        when(authenticateUser.execute(new AuthenticateUserCommand("arthur@prumo.io", "s3cret-pass")))
                .thenReturn(user);

        Authentication result = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("arthur@prumo.io", "s3cret-pass")
        );

        assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void translatesInvalidCredentialsIntoBadCredentials() {
        when(authenticateUser.execute(new AuthenticateUserCommand("arthur@prumo.io", "wrong-pass")))
                .thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("arthur@prumo.io", "wrong-pass")
        )).isInstanceOf(BadCredentialsException.class).hasMessage("Invalid email or password");
    }

    @Test
    void supportsOnlyUsernamePasswordTokens() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(TestingAuthenticationToken.class)).isFalse();
    }
}
