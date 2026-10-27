package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.PlatformRole;
import com.petlis.common.security.UserPrincipal;
import com.petlis.common.security.UserRoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    UserRoleService roles;
    SessionService sessions;
    ReviewService reviews;
    AdminService service;

    @BeforeEach
    void setup() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        roles = mock(UserRoleService.class); sessions = mock(SessionService.class); reviews = mock(ReviewService.class);
        service = new AdminService(store, roles, sessions, reviews);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(42, null, null, null), null, List.of()));
        when(roles.roles(42)).thenReturn(Set.of(PlatformRole.ADMIN));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void userListIsZeroBasedAndFiltersRealRoleAndHospital() {
        when(store.page(contains("lin_user"), contains("lin_user"), eq(0), eq(20), any(Object[].class)))
                .thenReturn(Map.of("items", List.of(Map.of("id", 7, "username", "doctor", "roleCodes", "DOCTOR,HOSPITAL_ADMIN")),
                        "total", 1L, "page", 0, "size", 20));

        var result = service.users("ACTIVE", "DOCTOR", 12, 0, 20);

        assertEquals(0, result.get("page"));
        assertEquals(List.of("DOCTOR", "HOSPITAL_ADMIN"), ((List<?>) result.get("items")).isEmpty() ? List.of() : ((Map<?, ?>) ((List<?>) result.get("items")).get(0)).get("roles"));
        verify(store).page(contains("u.status='normal'"), contains("r.role_code='DOCTOR'"), eq(0), eq(20), any(Object[].class));
    }

    @Test
    void disablingUserRevokesExistingSessions() {
        when(store.one(contains("FOR UPDATE"), eq(7))).thenReturn(Map.of("id", 7, "status", "normal"));
        when(jdbc.update(contains("SET status='hidden'"), eq(7))).thenReturn(1);

        service.setUserStatus(7, false, "停用测试");

        verify(sessions).revoke(7);
        verify(jdbc).update(contains("lin_audit_record"), eq(42), eq("USER_DISABLED"), eq("USER"), eq("7"), eq("停用测试"));
    }

    @Test
    void administratorCannotDisableOrReviewSelf() {
        assertThrows(BusinessException.class, () -> service.setUserStatus(42, false, "self"));
        assertThrows(BusinessException.class, () -> service.reviewUser(42, new ReviewService.Decision(true, "")));
        verifyNoInteractions(reviews);
    }

    @Test
    void hospitalReviewUpdatesExistingHospitalAndWritesAudit() {
        when(store.one(contains("lin_lb_infohospital"), eq(12))).thenReturn(Map.of("InfoHospitalId", 12, "IsActive", 0));

        service.reviewHospital(12, new ReviewService.Decision(true, "通过"));

        verify(jdbc).update("UPDATE lin_lb_infohospital SET IsActive=? WHERE InfoHospitalId=?", 1, 12);
        verify(jdbc).update(contains("lin_audit_record"), eq(42), eq("HOSPITAL_APPROVED"), eq("HOSPITAL"), eq("12"), eq("通过"));
    }

    @Test
    void nonAdminCannotUseManagementService() {
        when(roles.roles(42)).thenReturn(Set.of(PlatformRole.HOSPITAL_ADMIN));
        assertThrows(BusinessException.class, () -> service.hospitals("ACTIVE", 0, 20));
    }
}
