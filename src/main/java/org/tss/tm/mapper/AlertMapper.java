package org.tss.tm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tss.tm.dto.tenant.response.AlertResponse;
import org.tss.tm.entity.tenant.Alert;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(target = "scenarioName", source = "scenario.scenarioName")
    @Mapping(target = "customerName", expression = "java(alert.getCustomer().getFirstName() + \" \" + alert.getCustomer().getLastName())")
    @Mapping(target = "customerCode", source = "customer.cif")
    @Mapping(target = "customerIncome", source = "customer.income")
    AlertResponse toResponse(Alert alert);

    List<AlertResponse> toResponseList(List<Alert> alerts);
}
