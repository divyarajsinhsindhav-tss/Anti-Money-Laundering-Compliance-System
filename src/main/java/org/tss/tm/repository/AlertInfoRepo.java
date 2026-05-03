package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tss.tm.entity.tenant.Alert;
import org.tss.tm.entity.tenant.AlertInfo;
import org.tss.tm.entity.tenant.Customer;
import org.tss.tm.entity.tenant.FinancialTransaction;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AlertInfoRepo extends JpaRepository<AlertInfo, UUID> {
    @Query("SELECT a.transaction.transactionId FROM AlertInfo a WHERE a.scenario.scenarioId = :scenarioId AND a.transaction.transactionId IN :txnIds")
    Set<UUID> findAlreadyFlaggedTransactions(@Param("scenarioId") UUID scenarioId, @Param("txnIds") List<UUID> txnIds);

    @Query("SELECT ai FROM AlertInfo ai " +
           "LEFT JOIN FETCH ai.transaction t " +
           "LEFT JOIN FETCH t.account " +
           "WHERE ai.alert.alertCode = :alertCode")
    List<AlertInfo> findAllByAlert_AlertCode(@Param("alertCode") String alertCode);

    @Query("""
    SELECT ai FROM AlertInfo ai
    LEFT JOIN FETCH ai.transaction t
    LEFT JOIN FETCH t.account
    LEFT JOIN FETCH ai.rule r
    WHERE ai.alert.alertCode = :alertCode
    ORDER BY t.txnTimestamp DESC
""")
    List<AlertInfo> findDetailedByAlertCode(@Param("alertCode") String alertCode);
}
