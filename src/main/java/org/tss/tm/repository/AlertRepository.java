package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.tenant.Alert;

import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
}
