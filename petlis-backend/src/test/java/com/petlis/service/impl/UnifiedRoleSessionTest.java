package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.PlatformRole;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserRoleService;
import com.petlis.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UnifiedRoleSessionTest {
    JdbcTemplate jdbc;
    FeatureStore store;
    JwtUtil jwt;
    UserRoleService roles;
    SessionService sessions;

    @BeforeEach
    void setup() {
        jdbc = mock(JdbcTemplate.class);
        store = new FeatureStore(jdbc, new ObjectMapper());
        jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "unit-test-secret-only-0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwt, "expiration", 60000L);
        roles = mock(UserRoleService.class);
        sessions = new SessionService(store, jwt, new ReviewPermission(roles), roles);
    }

    private Map<String, Object> user(String status, String reviewStatus) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 7); row.put("username", "admin"); row.put("nickname", "平台管理员");
        row.put("status", status); row.put("review_status", reviewStatus); row.put("credential_version", 0);
        row.put("hospital_id", null); row.put("CityId", 1); row.put("hospital_active", 1);
        when(jdbc.queryForList(contains("FROM lin_user"), eq(7))).thenReturn(List.of(row));
        return row;
    }

    @Test
    void passwordSessionReturnsMappedAdminAndUserWithoutHospital() {
        user("normal", null);
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.ADMIN));

        var response = sessions.issue(7);

        assertTrue(jwt.isReviewerToken(response.getToken()));
        assertEquals(Set.of("ADMIN"), response.getRoles());
        assertEquals(7, response.getUser().get("id"));
        assertEquals("平台管理员", response.getUser().get("displayName"));
        assertTrue(sessions.valid(response.getToken()));
    }

    @Test
    void disabledAndRejectedUsersLoseOldTokensImmediately() {
        user("normal", "approved");
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.DOCTOR));
        Map<String, Object> row = user("normal", "approved");
        row.put("hospital_id", 12);
        String token = jwt.generateToken(7, null, 12, 1);
        assertTrue(sessions.valid(token));

        row.put("status", "hidden");
        assertFalse(sessions.valid(token));
        row.put("status", "normal"); row.put("review_status", "rejected");
        assertFalse(sessions.valid(token));
    }

    @Test
    void courierCanLoginAndValidateStaffSessionWithoutHospital() {
        user("normal", null);
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.COURIER));

        var response = sessions.issue(7);

        assertTrue(jwt.isStaffToken(response.getToken()));
        assertFalse(response.isReviewer());
        assertNull(response.getHospitalId());
        assertTrue(sessions.valid(response.getToken()));
    }

    @Test
    void labStaffCanLoginAndValidateStaffSessionWithoutHospital() {
        user("normal", null);
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.LAB_STAFF));

        var response = sessions.issue(7);

        assertTrue(jwt.isStaffToken(response.getToken()));
        assertTrue(sessions.valid(response.getToken()));
    }

    @Test
    void doctorAndHospitalAdminStillRequireHospital() {
        user("normal", null);
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.DOCTOR));
        assertThrows(com.petlis.common.exception.BusinessException.class, () -> sessions.issue(7));
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.HOSPITAL_ADMIN));
        assertThrows(com.petlis.common.exception.BusinessException.class, () -> sessions.issue(7));
    }

    @Test
    void doctorTokenIsInvalidForNonDoctorMappedRole() {
        Map<String, Object> row = user("normal", "approved");
        row.put("hospital_id", 12);
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.LAB_STAFF));

        assertFalse(sessions.valid(jwt.generateToken(7, null, 12, 1)));
    }

    @Test
    void inactiveHospitalBlocksLoginAndExistingToken() {
        Map<String, Object> row = user("normal", "approved");
        row.put("hospital_id", 12); row.put("hospital_active", 0);
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.DOCTOR));

        assertThrows(com.petlis.common.exception.BusinessException.class, () -> sessions.issue(7));
        assertFalse(sessions.valid(jwt.generateToken(7, null, 12, 1)));
    }

    @Test
    void revokedHospitalTokenCannotReplayAfterReenable() {
        Map<String, Object> row = user("normal", "approved");
        row.put("hospital_id", 12);
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.DOCTOR));
        String token = jwt.generateToken(7, null, 12, 1);
        assertTrue(sessions.valid(token));
        row.put("hospital_active", 0);
        assertFalse(sessions.valid(token));
        row.put("hospital_active", 1);
        assertTrue(sessions.valid(token));
        row.put("credential_version", 1);
        assertFalse(sessions.valid(token));
    }

    @Test
    void ordinaryMappedDoctorCannotForgeReviewerToken() {
        Map<String, Object> row = user("normal", "approved");
        row.put("hospital_id", 12);
        when(roles.roles(7)).thenReturn(Set.of(PlatformRole.DOCTOR));

        assertFalse(sessions.valid(jwt.generateReviewerToken(7, 0)));
    }
}
