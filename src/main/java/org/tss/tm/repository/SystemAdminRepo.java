package org.tss.tm.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.system.SystemAdmin;

public interface SystemAdminRepo extends JpaRepository<SystemAdmin, UUID> {
    Optional<SystemAdmin> findByEmail(String email);
}
