package pw.cris.cuadremos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pw.cris.cuadremos.domain.model.Group;

import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
}
