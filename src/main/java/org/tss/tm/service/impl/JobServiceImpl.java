package org.tss.tm.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.repository.JobRepo;
import org.tss.tm.repository.TenantRepo;
import org.tss.tm.service.interfaces.JobService;
import org.tss.tm.service.interfaces.TenantService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Service
public class JobServiceImpl implements JobService {
    private JobRepo jobRepo;
    private TenantService tenantService;

    @Override
    public JobRecord createNewJob(JobType type) {
        JobRecord newJob = JobRecord.builder()
                .jobType(type)
                .status(JobStatus.PENDING)
                .tenant(tenantService.getCurrentTenant())
                .startedAt(LocalDateTime.now())
                .build();
        return jobRepo.save(newJob);
    }

    @Override
    @Transactional
    public void updateJobStatus(UUID jobId, JobStatus status) {
        JobRecord job = jobRepo.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setStatus(status);
    }

    @Override
    @Transactional
    public void setCompletionTime(UUID jobId) {
        JobRecord job = jobRepo.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setCompletedAt(LocalDateTime.now());
    }
}
