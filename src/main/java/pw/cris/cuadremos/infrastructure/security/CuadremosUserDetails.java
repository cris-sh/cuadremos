package pw.cris.cuadremos.infrastructure.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adapts a Cuadremos user to the shape Spring Security expects.
 * The id is kept because it is later embedded in the JWT subject claim.
 */
@Getter
@RequiredArgsConstructor
public class CuadremosUserDetails implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    /** Never expose the password hash, not even in logs. */
    @Override
    public String toString() {
        return "CuadremosUserDetails[username=" + username + "]";
    }
}
