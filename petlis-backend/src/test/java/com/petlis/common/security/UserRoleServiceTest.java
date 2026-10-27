package com.petlis.common.security;

import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserRoleServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UserRoleService service = new UserRoleService(jdbc);

    @Test
    void readsMultipleActiveRoleAssignments() {
        when(jdbc.queryForList(contains("lin_user_role"), eq(7))).thenReturn(List.of(
                Map.of("id", 1L, "user_id", 7, "role_code", "DOCTOR", "hospital_id", 12, "scope_type", "HOSPITAL", "scope_id", 12),
                Map.of("id", 2L, "user_id", 7, "role_code", "LAB_STAFF", "laboratory_id", 31, "scope_type", "LABORATORY", "scope_id", 31)));

        assertEquals(Set.of(PlatformRole.DOCTOR, PlatformRole.LAB_STAFF), service.roles(7));
        assertEquals(2, service.assignments(7).size());
    }

    @Test
    void persistsRoleHospitalLaboratoryAndScope() {
        service.assign(7, "LAB_STAFF", null, 31, 42);

        verify(jdbc).update(contains("INSERT INTO lin_user_role"),
                eq(7), eq("LAB_STAFF"), isNull(), eq(31), eq("LABORATORY"), eq(31), eq(42));
    }

    @Test
    void rejectsInvalidRoleAndMissingRequiredScope() {
        assertThrows(BusinessException.class, () -> service.assign(7, "SUPER_USER", null, null, 42));
        assertThrows(BusinessException.class, () -> service.assign(7, "HOSPITAL_ADMIN", null, null, 42));
        assertThrows(BusinessException.class, () -> service.assign(7, "LAB_STAFF", 12, null, 42));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void roleRemovalDeactivatesOnlySelectedAssignment() {
        when(jdbc.update(contains("SET is_active=0"), eq(9L), eq(7))).thenReturn(1);
        service.remove(9L, 7);

        verify(jdbc).update(contains("SET is_active=0"), eq(9L), eq(7));
    }
}
