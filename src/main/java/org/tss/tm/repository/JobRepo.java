package org.tss.tm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tss.tm.entity.system.JobExecution;

import java.util.UUID;

public interface JobRepo extends JpaRepository<JobExecution, UUID> {
}
