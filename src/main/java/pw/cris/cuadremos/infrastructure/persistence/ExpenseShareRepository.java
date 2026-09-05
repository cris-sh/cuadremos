package pw.cris.cuadremos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pw.cris.cuadremos.domain.model.ExpenseShare;

import java.util.List;
import java.util.UUID;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, UUID> {
    List<ExpenseShare> findByExpenseId(UUID expenseId);
}
