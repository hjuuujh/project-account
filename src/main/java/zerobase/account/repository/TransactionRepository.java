package zerobase.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zerobase.account.domain.Transaction;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
}
