package org.tss.tm.service.interfaces;

import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.entity.system.JobRecord;

import java.util.UUID;

public interface JobService {
    JobRecord createNewJob(JobType type);
    void updateJobStatus(UUID jobId, JobStatus status);
}
