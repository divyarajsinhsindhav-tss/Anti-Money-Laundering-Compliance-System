package org.tss.tm.dto.tenant.response;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseDetailResponse {
    private CaseResponse caseResponse;
    private List<AlertResponse> alerts;
}
