package org.tss.tm.service.interfaces;

import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.dto.tenant.response.FileErrorResponse;
import org.tss.tm.dto.tenant.response.TenantAvailableResponse;
import org.tss.tm.dto.tenant.response.TenantResponse;
import org.tss.tm.entity.system.Tenant;

import org.tss.tm.dto.tenant.response.ScenarioResponse;
import org.tss.tm.dto.tenant.response.TenantDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import org.tss.tm.dto.tenant.response.TransactionDashboardResponse;

public interface TenantService {
    TenantResponse createTenant(TenantRegistrationRequest request, String email);

    TenantAvailableResponse tenantAvailable(String tenantCode);

    Tenant getTenantByName(String tenantName);

    List<TenantResponse> getAllTenants();

    void migrateAllTenants();

    Tenant getCurrentTenant();

    String getTenantCodeBySchemaName(String schemaName);

    Page<ScenarioResponse> getScenarios(Pageable pageable);

    Page<FileErrorResponse> getFileError(Pageable pageable);

    TenantDetailResponse getTenantDetail(String tenantCode);

    TransactionDashboardResponse getTransactionDashboardStats();
    List<TransactionDashboardResponse.RecentJobResponse> getTransactionJobs();
}
