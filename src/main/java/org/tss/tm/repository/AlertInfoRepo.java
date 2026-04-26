package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tss.tm.entity.tenant.AlertInfo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AlertInfoRepo extends JpaRepository<AlertInfo, UUID> {
    @Query("SELECT a.transaction.transactionId FROM AlertInfo a WHERE a.scenario.scenarioId = :scenarioId AND a.transaction.transactionId IN :txnIds")
    Set<UUID> findAlreadyFlaggedTransactions(@Param("scenarioId") UUID scenarioId, @Param("txnIds") List<UUID> txnIds);
}
