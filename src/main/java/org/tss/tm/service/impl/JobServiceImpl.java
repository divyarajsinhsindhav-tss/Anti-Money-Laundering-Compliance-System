package org.tss.tm.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.entity.system.JobExecution;
import org.tss.tm.entity.system.Tenant;
import org.tss.tm.repository.JobRepo;
import org.tss.tm.repository.TenantRepo;
import org.tss.tm.service.interfaces.JobService;
import org.tss.tm.service.interfaces.TenantService;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Service
public class JobServiceImpl implements JobService {
    private JobRepo jobRepo;
    private TenantService tenantService;
    private TenantRepo tenantRepo;

    @Override
    public JobExecution createNewJob(JobType type) {
        JobExecution newJob = new JobExecution();
        newJob.setJobType(type);
        newJob.setStatus(JobStatus.PENDING);
        newJob.setTenant(tenantService.getCurrentTenant());
//        CHANGE THIS AT PRODUCTION-----------
//        TESTING ONLY:
//        Tenant tempTenant=tenantRepo.getReferenceById(UUID.fromString("c77290af-8631-422b-a9a6-0d4ebac6ced9"));
//        newJob.setTenant(tempTenant);

//        ------------------

        newJob.setStartedAt(LocalDateTime.now());

        return jobRepo.save(newJob);
    }

    @Override
    @Transactional
    public void updateJobStatus(UUID jobId, JobStatus status) {
        JobExecution job = jobRepo.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setStatus(status);
    }
}
