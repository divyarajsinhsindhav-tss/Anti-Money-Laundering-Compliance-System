package org.tss.tm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tss.tm.common.constant.FileConstants;
import org.tss.tm.common.enums.JobStatus;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.service.interfaces.FileService;

import org.tss.tm.service.interfaces.JobService;
import org.tss.tm.service.interfaces.TenantService;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private FileProcessor fileProcessor;

    @Autowired
    private JobService jobService;

    @Autowired
    private TenantService tenantService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public CompletableFuture<String> processFile(MultipartFile newFile, JobType type) {
        log.info("File Processing Started.");
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        UUID jobId = jobService.createNewJob(type).getJobId();
        log.info("New Job Created.");

        File file = new File(uploadDir,
                System.currentTimeMillis() + "_" + newFile.getOriginalFilename());

        try {
            newFile.transferTo(file);

            UUID currentTenantId= tenantService.getCurrentTenant().getTenantId();
//            UUID currentTenantId = UUID.fromString("c77290af-8631-422b-a9a6-0d4ebac6ced9");
//            WE HAVE TO CHANGE THE UUID RANDOM and CREATENEWJOB UUID--------
            log.info("Java File Processing Completed");

            if (type.equals(JobType.UPLOADING_TRANSACTION)) {
                validateHeader(file, FileConstants.expectedTransactionHeader);
                fileProcessor.loadTransactionCsv(jobId, file, currentTenantId);
            } else if (type.equals(JobType.UPLOADING_CUSTOMER)) {
                validateHeader(file, FileConstants.expectedCustomerHeader);
                fileProcessor.loadCustomerFile(jobId, file, currentTenantId);
            } else {
                throw new IllegalArgumentException("Unsupported Job Type");
            }

            log.info("File Uploaded Successfully.");
            return CompletableFuture.completedFuture(String.valueOf(jobId));

            //HERE WE HAVE TO MAKE JOB CODE AND RETURN THAT INSTEAD OF ID.

        } catch (Exception e) {
            if (file.exists()) {
                file.delete();
            }
            jobService.updateJobStatus(jobId, JobStatus.FAILED);
            log.error("File pre-processing failed for job {}", jobId);
            throw new RuntimeException("File Upload Failed");
        }
    }


    private void validateHeader(File file, List<String> expected) {

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String headerLine = br.readLine();

            if (headerLine == null) {
                throw new RuntimeException("Empty file");
            }

            List<String> actual = Arrays.stream(headerLine.split(","))
                    .map(h -> h.trim()
                            .replace("\"", "")
                            .toLowerCase())
                    .toList();

            List<String> normalizedExpected = expected.stream()
                    .map(String::toLowerCase)
                    .toList();

            if (!actual.equals(normalizedExpected)) {
                throw new RuntimeException(
                        "Invalid header format. expected=" + expected + " actual=" + actual
                );
            }

        } catch (IOException e) {
            throw new RuntimeException("Header validation failed", e);
        }
    }

}
