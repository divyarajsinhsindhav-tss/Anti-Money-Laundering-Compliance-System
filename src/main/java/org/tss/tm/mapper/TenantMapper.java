package org.tss.tm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tss.tm.dto.tenant.request.TenantRegistrationRequest;
import org.tss.tm.dto.tenant.response.JobRecordResponse;
import org.tss.tm.dto.tenant.response.TenantResponse;
import org.tss.tm.entity.system.JobRecord;
import org.tss.tm.entity.system.Tenant;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "onboardedByAdmin", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Tenant toEntity(TenantRegistrationRequest request);

    TenantResponse toResponse(Tenant tenant);

    List<TenantResponse> toResponseList(List<Tenant> tenants);

    JobRecordResponse toJobResponse(JobRecord jobRecord);

    List<JobRecordResponse> toJobResponseList(List<JobRecord> jobRecords);
}
