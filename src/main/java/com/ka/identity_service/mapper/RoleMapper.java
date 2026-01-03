package com.ka.identity_service.mapper;

import com.ka.identity_service.dto.request.PermissionRequest;
import com.ka.identity_service.dto.request.RoleRequest;
import com.ka.identity_service.dto.response.PermissionResponse;
import com.ka.identity_service.dto.response.RoleResponse;
import com.ka.identity_service.entity.Permission;
import com.ka.identity_service.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);
    RoleResponse toRoleResponse(Role role);
}
