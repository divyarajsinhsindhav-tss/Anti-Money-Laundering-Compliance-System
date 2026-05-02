package org.tss.tm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tss.tm.dto.tenant.response.CaseResponse;
import org.tss.tm.entity.tenant.AmlCase;

@Mapper(componentModel = "spring")
public interface CaseMapper {

    @Mapping(target = "createdByEmail", source = "createdBy.email")
    @Mapping(target = "assignedToUserCode", source = "assignedTo.userCode")
    @Mapping(target = "assignedToEmail", source = "assignedTo.email")

    CaseResponse toResponse(AmlCase amlCase);
}
