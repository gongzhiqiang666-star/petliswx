package com.petlis.common.security;

import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessServiceTest {
    private final UserRoleService roles = mock(UserRoleService.class);
    private final AccessService access = new AccessService(roles);

    private void login(int userId, Integer hospitalId, List<UserRoleAssignment> assignments) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(userId, null, hospitalId, 1), null, List.of()));
        when(roles.assignments(userId)).thenReturn(assignments);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void hospitalAdminAndLabStaffRolesUnionTheirScopes() {
        login(7, 12, List.of(
                new UserRoleAssignment(1L, 7, "HOSPITAL_ADMIN", 12, null, "HOSPITAL", 12),
                new UserRoleAssignment(2L, 7, "LAB_STAFF", null, 31, "LABORATORY", 31)));

        CurrentActor actor = access.current();

        assertEquals(Set.of(PlatformRole.HOSPITAL_ADMIN, PlatformRole.LAB_STAFF), actor.roles());
        assertTrue(actor.canAccessHospital(12));
        assertTrue(actor.canAccessLaboratory(31));
        assertFalse(actor.canAccessHospital(13));
        assertFalse(actor.canAccessLaboratory(32));
        assertDoesNotThrow(() -> access.requireHospital(12));
        assertDoesNotThrow(() -> access.requireLaboratory(31));
        assertThrows(BusinessException.class, () -> access.requireHospital(13));
        assertThrows(BusinessException.class, () -> access.requireLaboratory(32));
    }

    @Test
    void courierOnlyAccessesOwnAssignments() {
        login(7, null, List.of(new UserRoleAssignment(1L, 7, "COURIER", null, null, "COURIER", 7)));

        assertDoesNotThrow(() -> access.requireCourierAssignment(7));
        assertThrows(BusinessException.class, () -> access.requireCourierAssignment(8));
    }

    @Test
    void adminHasGlobalScope() {
        login(7, null, List.of(new UserRoleAssignment(1L, 7, "ADMIN", null, null, "GLOBAL", 0)));
        CurrentActor actor = access.current();

        assertTrue(actor.dataScope().global());
        assertTrue(actor.canAccessHospital(999));
        assertTrue(actor.canAccessLaboratory(999));
        assertDoesNotThrow(() -> access.requireCourierAssignment(999));
    }

    @Test
    void managementBoundaryAcceptsOnlyAdmin() {
        login(7, null, List.of(new UserRoleAssignment(1L, 7, "ADMIN", null, null, "GLOBAL", 0)));
        assertDoesNotThrow(access::requireAdmin);
        login(8, 12, List.of(new UserRoleAssignment(2L, 8, "HOSPITAL_ADMIN", 12, null, "HOSPITAL", 12)));
        assertThrows(BusinessException.class, access::requireAdmin);
    }

    @Test
    void legacyDoctorWithoutMappingKeepsOwnHospitalScope() {
        login(7, 12, List.of());

        CurrentActor actor = access.current();

        assertEquals(Set.of(PlatformRole.DOCTOR), actor.roles());
        assertTrue(actor.canAccessHospital(12));
        assertFalse(actor.canAccessHospital(13));
    }
}
