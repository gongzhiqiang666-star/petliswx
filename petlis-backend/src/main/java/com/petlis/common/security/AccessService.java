package com.petlis.common.security;

import com.petlis.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccessService {
    private final UserRoleService userRoles;

    public CurrentActor current() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        List<UserRoleAssignment> assignments = userRoles.assignments(principal.getUserId());
        Set<PlatformRole> roles = new HashSet<>();
        Set<Integer> hospitals = new HashSet<>();
        Set<Integer> laboratories = new HashSet<>();
        for (UserRoleAssignment assignment : assignments) {
            roles.add(assignment.role());
            if (assignment.hospitalId() != null) hospitals.add(assignment.hospitalId());
            if (assignment.laboratoryId() != null) laboratories.add(assignment.laboratoryId());
        }
        if (roles.isEmpty()) {
            roles.add(PlatformRole.DOCTOR);
            if (principal.getHospitalId() != null) hospitals.add(principal.getHospitalId());
        }
        boolean global = roles.contains(PlatformRole.ADMIN);
        DataScope scope = new DataScope(global, hospitals, laboratories,
                roles.contains(PlatformRole.COURIER) ? principal.getUserId() : null);
        return new CurrentActor(principal.getUserId(), roles, hospitals, laboratories, scope);
    }

    public void requireAdmin() {
        if (!current().isAdmin()) throw new BusinessException(403, "需要平台管理员权限");
    }

    public void requireHospitalAdmin(Integer hospitalId) {
        var actor=current();
        if (!actor.isAdmin() && !managedHospitalIds().contains(hospitalId)) throw forbidden();
    }

    public Set<Integer> managedHospitalIds() {
        Set<Integer> result=new HashSet<>();
        for(var assignment:userRoles.assignments(SecurityUtils.getCurrentUserId())) {
            if(assignment.role()==PlatformRole.HOSPITAL_ADMIN && assignment.hospitalId()!=null
                    && "HOSPITAL".equals(assignment.scopeType()) && assignment.hospitalId().equals(assignment.scopeId())) result.add(assignment.hospitalId());
        }
        return Set.copyOf(result);
    }

    public void requireHospital(Integer hospitalId) {
        if (!current().canAccessHospital(hospitalId)) throw forbidden();
    }

    public void requireLaboratory(Integer laboratoryId) {
        if (!current().canAccessLaboratory(laboratoryId)) throw forbidden();
    }

    public void requireCourierAssignment(Integer courierUserId) {
        if (!current().canAccessCourierAssignment(courierUserId)) throw forbidden();
    }

    private BusinessException forbidden() { return new BusinessException(403, "无权访问该数据范围"); }
}
