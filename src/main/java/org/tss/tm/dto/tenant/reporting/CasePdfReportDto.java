package org.tss.tm.dto.tenant.reporting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CasePdfReportDto {

    private CaseReportDto caseInfo;
    private CustomerReportDto customer;
    private List<AlertReportDto> alerts;
}