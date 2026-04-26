package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.tenant.AmlCase;

import java.util.UUID;

public interface CaseRepo extends JpaRepository<AmlCase, UUID> {
}
