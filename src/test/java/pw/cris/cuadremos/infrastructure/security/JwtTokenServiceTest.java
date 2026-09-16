package pw.cris.cuadremos.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class JwtTokenServiceTest {

    private static final String TEST_SECRET = "O/UEEokySClzgy1WP3su/ytcjndOCxsKjsTsNFSWFdC1o4av8+c/OqFRAxNju1v8vNhK7uUZDVIXnsSgW6zqcw==";
    private static final String PASSWORD_HASH = "$argon2id$v=19$m=65536,t=3,p=1$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG";

    private JwtTokenService tokenService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(
                TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
        );

        tokenService = new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)));
        jwtDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private CuadremosUserDetails user(UUID id) {
        return new CuadremosUserDetails(id, "test.user", PASSWORD_HASH);
    }

    @Test
    @DisplayName("puts the user id in the subject claim")
    void putsUserIdInSubject() {
        UUID id = UUID.randomUUID();

        Jwt decoded = jwtDecoder.decode(tokenService.generateToken(user(id)));

        assertThat(decoded.getSubject()).isEqualTo(id.toString());
        assertThat(decoded.getClaimAsString("username")).isEqualTo("test.user");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("cuadremos");
    }

    @Test
    @DisplayName("signs the token with HS256")
    void signsWithHs256() {
        Jwt decoded = jwtDecoder.decode(tokenService.generateToken(user(UUID.randomUUID())));

        assertThat(decoded.getHeaders()).containsEntry("alg", MacAlgorithm.HS256.toString());
    }

    @Test
    @DisplayName("issues a token that expires within the hour")
    void issuesTokenThatExpires() {
        Jwt decoded = jwtDecoder.decode(tokenService.generateToken(user(UUID.randomUUID())));

        assertThat(decoded.getExpiresAt())
                .isNotNull()
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(61)));
    }

    @Test
    @DisplayName("never puts the password hash inside the token")
    void neverIncludesPasswordHash() {
        Jwt decoded = jwtDecoder.decode(tokenService.generateToken(user(UUID.randomUUID())));

        assertThat(decoded.getClaims().toString()).doesNotContain(PASSWORD_HASH);
    }

}
