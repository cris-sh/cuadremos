package pw.cris.cuadremos.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Issues signed JWTs for authenticated users.
 * It does not verify credentials; that happens before this is called.
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final Duration TOKEN_TTL = Duration.ofHours(1);
    private static final String ISSUER = "cuadremos";

    private final JwtEncoder jwtEncoder;

    public String generateToken(CuadremosUserDetails user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .build();

        // NimbusJwtEncoder defaults to RS256, which cannot sign with an HMAC key.
        // The algorithm must be stated explicitly.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Duration tokenTtl() {
        return TOKEN_TTL;
    }
}
