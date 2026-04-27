package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileErrorResponse {
    private Long errorId;
    private String jobId;
    private String identifier; // cif or txn_no
    private String rawRow;
    private List<String> criticalErrors;
    private List<String> warningErrors;
    private LocalDateTime createdAt;
    private String sourceType; // CUSTOMER or TRANSACTION
}
