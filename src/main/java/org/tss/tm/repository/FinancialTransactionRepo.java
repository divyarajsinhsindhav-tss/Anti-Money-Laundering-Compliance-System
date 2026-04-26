package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.tenant.FinancialTransaction;

import java.util.UUID;

public interface FinancialTransactionRepo extends JpaRepository<FinancialTransaction, UUID> {
}
