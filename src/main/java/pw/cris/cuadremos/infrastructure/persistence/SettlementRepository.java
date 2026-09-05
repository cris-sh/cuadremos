package pw.cris.cuadremos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pw.cris.cuadremos.domain.model.Settlement;

import java.util.List;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findByGroupId(UUID groupId);
}
