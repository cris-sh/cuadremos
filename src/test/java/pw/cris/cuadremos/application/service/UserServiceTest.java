package pw.cris.cuadremos.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pw.cris.cuadremos.application.dto.RegisterRequest;
import pw.cris.cuadremos.domain.exception.EmailAlreadyExistsException;
import pw.cris.cuadremos.domain.exception.UsernameAlreadyExistsException;
import pw.cris.cuadremos.domain.model.User;
import pw.cris.cuadremos.infrastructure.persistence.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String RAW_PASSWORD = "supersecret123";

    @Mock
    private UserRepository userRepository;

    /* The real encoder, so this test also exercises the BouncyCastle Argon2 implementation */
    private final PasswordEncoder passwordEncoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    private RegisterRequest request(String username, String email) {
        return new RegisterRequest(username, "cris", email, RAW_PASSWORD);
    }

    private void acceptNewUser() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    private User savedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("stores the password hashed with argon2id, never in plain text")
    void storesPasswordHashed() {
        acceptNewUser();

        userService.register(request("cris", "cris@example.com"));

        String stored = savedUser().getPassword();

        assertThat(stored).doesNotContain(RAW_PASSWORD);
        assertThat(stored).startsWith("$argon2id$");
        assertThat(passwordEncoder.matches(RAW_PASSWORD, stored)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", stored)).isFalse();
    }

    @Test
    @DisplayName("stores username and email lowercased")
    void storedUsernameAndEmailLowercased() {
        acceptNewUser();

        userService.register(request("Cris", "Cris@example.com"));

        User saved = savedUser();

        assertThat(saved.getUsername()).isEqualTo("cris");
        assertThat(saved.getEmail()).isEqualTo("cris@example.com");
    }

    @Test
    @DisplayName("checks username availability using the normalised form")
    void checksUsernameAvailabilityNormalised() {
        when(userRepository.existsByUsername("cris")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request("Cris", "cris@example.com")))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("checks email availability using the normalised form")
    void checksEmailAvailabilityNormalised() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("cris@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request("cris", "Cris@Example.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }



}
