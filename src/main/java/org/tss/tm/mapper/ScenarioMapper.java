package org.tss.tm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.tss.tm.dto.admin.response.ScenarioDetailResponse;
import org.tss.tm.dto.admin.response.ScenarioResponse;
import org.tss.tm.entity.system.Scenario;
import org.tss.tm.entity.system.TenantScenarioMapping;
import org.tss.tm.entity.system.Rule;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScenarioMapper {
    @Named("adminResponse")
    ScenarioResponse toResponse(Scenario scenario);

    @Named("tenantResponse")
    org.tss.tm.dto.tenant.response.ScenarioResponse toTenantResponse(Scenario scenario);

    @Mapping(target = "tenants", source = "tenantScenarios")
    @Mapping(target = "rules", source = "rules")
    ScenarioDetailResponse toDetailResponse(Scenario scenario);

    @Mapping(target = "tenantCode", source = "tenant.tenantCode")
    @Mapping(target = "name", source = "tenant.name")
    ScenarioDetailResponse.TenantInfo toTenantInfo(TenantScenarioMapping mapping);

    ScenarioDetailResponse.RuleInfo toRuleInfo(Rule rule);

    @org.mapstruct.IterableMapping(qualifiedByName = "adminResponse")
    List<ScenarioResponse> toResponseList(List<Scenario> scenarios);

    @org.mapstruct.IterableMapping(qualifiedByName = "tenantResponse")
    List<org.tss.tm.dto.tenant.response.ScenarioResponse> toTenantResponseList(List<Scenario> scenarios);
}
