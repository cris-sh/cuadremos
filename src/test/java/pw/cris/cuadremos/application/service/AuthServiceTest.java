package pw.cris.cuadremos.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import pw.cris.cuadremos.application.dto.LoginRequest;
import pw.cris.cuadremos.application.dto.TokenResponse;
import pw.cris.cuadremos.infrastructure.security.CuadremosUserDetails;
import pw.cris.cuadremos.infrastructure.security.JwtTokenService;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthService authService;

    private final CuadremosUserDetails user = new CuadremosUserDetails(UUID.randomUUID(), "test.user", "$argon2id$v=19$m=65536,t=3,p=1$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG");

    private Authentication authenticated() {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());
    }

    @Test
    @DisplayName("returns a bearer token for valid credentials")
    void returnsBearerTokenForValidCredentials() {
        when(authenticationManager.authenticate(any())).thenReturn(authenticated());
        when(jwtTokenService.generateToken(user)).thenReturn("signed-token");
        when(jwtTokenService.tokenTtl()).thenReturn(Duration.ofHours(1));

        TokenResponse response = authService.login(new LoginRequest("test.user", "secret-pass123"));

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
    }

    @Test
    @DisplayName("forwards the submitted credentials to the authentication manager")
    void forwardsSubmittedCredentials() {
        when(authenticationManager.authenticate(any())).thenReturn(authenticated());
        when(jwtTokenService.generateToken(user)).thenReturn("signed-token");
        when(jwtTokenService.tokenTtl()).thenReturn(Duration.ofHours(1));

        TokenResponse response = authService.login(new LoginRequest("test.user", "secret-pass123"));

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());

        assertThat(captor.getValue().getPrincipal()).isEqualTo("test.user");
        assertThat(captor.getValue().getCredentials()).isEqualTo("secret-pass123");
        assertThat(captor.getValue().isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("never issues a token when credentials are rejected")
    void neverIssuesTokenWhenCredentialsAreRejected() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test.user", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtTokenService, never()).generateToken(any());
    }

}
