package com.petlis.common.security;

import java.util.Set;

public record CurrentActor(
        Integer userId,
        Set<PlatformRole> roles,
        Set<Integer> hospitalIds,
        Set<Integer> laboratoryIds,
        DataScope dataScope
) {
    public CurrentActor {
        roles = Set.copyOf(roles);
        hospitalIds = Set.copyOf(hospitalIds);
        laboratoryIds = Set.copyOf(laboratoryIds);
    }

    public boolean hasRole(PlatformRole role) { return roles.contains(role); }
    public boolean isAdmin() { return hasRole(PlatformRole.ADMIN); }
    public boolean canAccessHospital(Integer hospitalId) {
        return dataScope.global() || hospitalId != null && hospitalIds.contains(hospitalId);
    }
    public boolean canAccessLaboratory(Integer laboratoryId) {
        return dataScope.global() || laboratoryId != null && laboratoryIds.contains(laboratoryId);
    }
    public boolean canAccessCourierAssignment(Integer courierUserId) {
        return dataScope.global() || hasRole(PlatformRole.COURIER) && userId.equals(courierUserId);
    }
}
