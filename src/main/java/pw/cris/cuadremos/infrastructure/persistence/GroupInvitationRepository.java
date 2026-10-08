package pw.cris.cuadremos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pw.cris.cuadremos.domain.model.GroupInvitation;

import java.util.UUID;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, UUID> {
}
