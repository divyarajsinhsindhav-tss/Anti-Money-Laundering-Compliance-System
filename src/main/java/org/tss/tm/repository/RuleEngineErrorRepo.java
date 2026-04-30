package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.tenant.RuleEngineError;

public interface RuleEngineErrorRepo extends JpaRepository<RuleEngineError,Long> {
}
