package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.tenant.AlertAudit;

import java.util.UUID;

public interface AlertAuditRepo extends JpaRepository<AlertAudit, UUID> {
}
