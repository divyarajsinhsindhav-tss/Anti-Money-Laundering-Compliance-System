package org.tss.tm.dto.tenant.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDashboardResponse {
    private long totalCustomers;
    private long totalTransactions;
    private long totalOpenAlerts;
    private long totalUnderReviewCases;
    private List<AlertResponse> recentAlerts;
    private List<CaseResponse> recentCases;
}
