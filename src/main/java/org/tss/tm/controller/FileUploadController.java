package org.tss.tm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tss.tm.entity.common.ValidationError;
import org.tss.tm.service.interfaces.FileService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<List<ValidationError>> uploadTransactionFile(
            @RequestParam("file") MultipartFile newFile) {

        try {
            CompletableFuture<List<ValidationError>> future = fileService.processFile(newFile);

            List<ValidationError> errors = future.get(); // blocks

            return ResponseEntity.ok(errors);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
