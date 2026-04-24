package org.tss.tm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tss.tm.dto.tenant.request.TenantAdminRegistrationRequest;
import org.tss.tm.dto.user.request.ComplianceOfficerRegistrationRequest;
import org.tss.tm.dto.user.response.UserResponse;
import org.tss.tm.dto.tenant.response.TenantUserResponse;
import org.tss.tm.entity.tenant.TenantUser;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userCode", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    TenantUser toEntity(ComplianceOfficerRegistrationRequest request);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userCode", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    TenantUser toEntity(TenantAdminRegistrationRequest request);

    UserResponse toResponse(TenantUser user);

    @Mapping(target = "name", source = "tenant.name")
    @Mapping(target = "displayName", source = "tenant.displayName")
    @Mapping(target = "tenantCode", source = "tenant.tenantCode")
    @Mapping(target = "email", source = "user.email")
    TenantUserResponse toTenantUserResponse(TenantUser user);

    List<UserResponse> toResponseList(List<TenantUser> users);
}
