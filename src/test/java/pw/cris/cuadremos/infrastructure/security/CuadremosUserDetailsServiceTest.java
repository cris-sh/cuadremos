package pw.cris.cuadremos.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pw.cris.cuadremos.domain.model.User;
import pw.cris.cuadremos.infrastructure.persistence.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuadremosUserDetailsServiceTest {

    private static final String PASSWORD_HASH = "$2a$10$7Q9J1Z1Z1Z1Z1Z1Z1Z1Z1OeW";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CuadremosUserDetailsService userDetailsService;

    private User existingUser(UUID id) {
        return User.builder()
                .id(id)
                .username("test.user")
                .name("Test User")
                .email("test@example.com")
                .password(PASSWORD_HASH)
                .build();
    }

    @Test
    @DisplayName("load an existing user keeps its id")
    void loadsExistingUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByUsername("test.user"))
                .thenReturn(Optional.of(existingUser(id)));

        UserDetails result = userDetailsService.loadUserByUsername("test.user");

        assertThat(result).isInstanceOf(CuadremosUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("test.user");
        assertThat(result.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(result.getAuthorities()).isEmpty();
        assertThat(((CuadremosUserDetails) result).getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("looks up the username in lowercase")
    void normalisesUsernameToLowercase() {
        when(userRepository.findByUsername("test.user"))
                .thenReturn(Optional.of(existingUser(UUID.randomUUID())));

        UserDetails result = userDetailsService.loadUserByUsername("TEST.USER");

        assertThat(result.getUsername()).isEqualTo("test.user");
    }

    @Test
    @DisplayName("fails when the user does not exist")
    void failsWhenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("does not leak the password hash in toString")
    void doesNotLeakPasswordHash() {
        when(userRepository.findByUsername("test.user"))
                .thenReturn(Optional.of(existingUser(UUID.randomUUID())));

        UserDetails result = userDetailsService.loadUserByUsername("test.user");

        assertThat(result.toString()).doesNotContain(PASSWORD_HASH);
        assertThat(result.toString()).contains("test.user");
    }
}
