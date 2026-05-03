package org.tss.tm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tss.tm.entity.tenant.FinancialTransaction;

import java.time.LocalDateTime;
import java.util.UUID;

public interface FinancialTransactionRepo extends JpaRepository<FinancialTransaction, UUID> {
    @Query("SELECT t FROM FinancialTransaction t WHERE " +
           "(LOWER(t.txnNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.account.accountNumber) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(cast(:startDate as timestamp) IS NULL OR t.txnTimestamp >= :startDate) AND " +
           "(cast(:endDate as timestamp) IS NULL OR t.txnTimestamp <= :endDate)")
    Page<FinancialTransaction> searchTransactions(
        @Param("search") String search, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate, 
        Pageable pageable);
}
