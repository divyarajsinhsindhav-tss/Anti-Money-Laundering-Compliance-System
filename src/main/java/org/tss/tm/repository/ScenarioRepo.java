package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tss.tm.entity.system.Scenario;

import java.util.List;
import java.util.UUID;

public interface ScenarioRepo extends JpaRepository<Scenario, UUID> {
    @Query("""
        SELECT s FROM Scenario s 
        JOIN s.tenantScenarios ts 
        WHERE s.status = 'ACTIVE' 
          AND ts.tenantId = :tenantId
    """)
    List<Scenario> findActiveScenariosByTenantId(@Param("tenantId") String tenantId);
}
