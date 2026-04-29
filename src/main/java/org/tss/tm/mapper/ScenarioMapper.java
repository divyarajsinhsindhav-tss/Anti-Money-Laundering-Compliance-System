package org.tss.tm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tss.tm.dto.admin.response.ScenarioDetailResponse;
import org.tss.tm.dto.admin.response.ScenarioResponse;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.system.TenantScenarioMapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScenarioMapper {
    ScenarioResponse toResponse(Scenario scenario);

    org.tss.tm.dto.tenant.response.ScenarioResponse toTenantResponse(Scenario scenario);
    
    @Mapping(target = "tenants", source = "tenantScenarios")
    ScenarioDetailResponse toDetailResponse(Scenario scenario);

    @Mapping(target = "tenantCode", source = "tenant.tenantCode")
    @Mapping(target = "name", source = "tenant.name")
    ScenarioDetailResponse.TenantInfo toTenantInfo(TenantScenarioMapping mapping);

    List<ScenarioResponse> toResponseList(List<Scenario> scenarios);

    List<org.tss.tm.dto.tenant.response.ScenarioResponse> toTenantResponseList(List<Scenario> scenarios);
}
