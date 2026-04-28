package org.tss.tm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tss.tm.common.enums.JobType;
import org.tss.tm.entity.common.ValidationError;
import org.tss.tm.service.interfaces.FileService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/file")
//@RequestMapping("/public/api/v1/file")
public class FileUploadController {

    @Autowired
    private FileService fileService;

    @PostMapping("/uploadTransaction")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<String> uploadTransactionFile(
            @RequestParam("file") MultipartFile newFile) {

        try {
            CompletableFuture<String> future = fileService.processFile(newFile, JobType.FILE_UPLOAD_TRANSACTION);

            return ResponseEntity.ok(future.get());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/uploadCustomer")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<String> uploadCustomerFile(
            @RequestParam("file") MultipartFile newFile) {

        try {
            CompletableFuture<String> future = fileService.processFile(newFile, JobType.FILE_UPLOAD_CUSTOMER);

            return ResponseEntity.ok(future.get());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
