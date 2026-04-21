package org.tss.tm.service.interfaces;

import org.springframework.web.multipart.MultipartFile;
import org.tss.tm.entity.common.ValidationError;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FileService {
    CompletableFuture<List<ValidationError>> processFile(MultipartFile newFile);
}
