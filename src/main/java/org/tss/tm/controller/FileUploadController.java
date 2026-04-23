package org.tss.tm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/public/api/files")
public class FileUploadController {

    @Autowired
    private FileService fileService;

    @PostMapping("/uploadTransaction")
    public ResponseEntity<String> uploadTransactionFile(
            @RequestParam("file") MultipartFile newFile) {

        try {
            CompletableFuture<String> future = fileService.processFile(newFile, JobType.UPLOADING_TRANSACTION);

            return ResponseEntity.ok(future.get());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/uploadCustomer")
    public ResponseEntity<String> uploadCustomerFile(
            @RequestParam("file") MultipartFile newFile) {

        try {
            CompletableFuture<String> future = fileService.processFile(newFile, JobType.UPLOADING_CUSTOMER);

            return ResponseEntity.ok(future.get());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
