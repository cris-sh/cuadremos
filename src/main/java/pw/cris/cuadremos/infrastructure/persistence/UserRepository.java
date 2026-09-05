package pw.cris.cuadremos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pw.cris.cuadremos.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
