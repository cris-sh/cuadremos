package pw.cris.cuadremos.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pw.cris.cuadremos.application.dto.LoginRequest;
import pw.cris.cuadremos.application.dto.TokenResponse;
import pw.cris.cuadremos.infrastructure.security.CuadremosUserDetails;
import pw.cris.cuadremos.infrastructure.security.JwtTokenService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    /**
     * Verifies the submitted credentials and issues a token.
     * throws AuthenticationException when they do not match.
     */
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(),
                        request.password()
                )
        );

        CuadremosUserDetails user = (CuadremosUserDetails) authentication.getPrincipal();

        return new TokenResponse(
                jwtTokenService.generateToken(user),
                TOKEN_TYPE,
                jwtTokenService.tokenTtl().toSeconds()
        );
    }
}
