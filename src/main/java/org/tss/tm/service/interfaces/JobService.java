package org.tss.tm.service.interfaces;

import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.entity.system.JobExecution;

import java.util.UUID;

public interface JobService {
    JobExecution createNewJob(JobType type);
    void updateJobStatus(UUID jobId, JobStatus status);
}
