package com.petlis.common.security;

public record UserRoleAssignment(
        Long id,
        Integer userId,
        String roleCode,
        Integer hospitalId,
        Integer laboratoryId,
        String scopeType,
        Integer scopeId
) {
    public PlatformRole role() { return PlatformRole.valueOf(roleCode); }
}
