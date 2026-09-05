package pw.cris.cuadremos.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pw.cris.cuadremos.domain.model.User;
import pw.cris.cuadremos.infrastructure.persistence.UserRepository;

import java.util.Locale;

/**
 * Spring Security calls this during login to load the credentials
 * of the user attempting to authenticate.
 */
@Service
@RequiredArgsConstructor
public class CuadremosUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Usernames are stored lowercased at registration time.
        // Locale.ROOT keeps the conversion stable regardless of the platform locale.
        User user = userRepository
                .findByUsername(username.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        return new CuadremosUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword()
        );
    }
}
