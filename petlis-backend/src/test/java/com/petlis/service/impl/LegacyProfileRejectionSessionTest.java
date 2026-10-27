package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserPrincipal;
import com.petlis.common.security.UserRoleService;
import com.petlis.common.util.JwtUtil;
import com.petlis.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegacyProfileRejectionSessionTest {
    JdbcTemplate jdbc;
    JwtUtil jwt;
    ReviewService reviews;
    SessionService sessions;
    AtomicInteger credentialVersion;

    @BeforeEach
    void setup() {
        jdbc = mock(JdbcTemplate.class);
        credentialVersion = new AtomicInteger(0);
        FeatureStore store = new FeatureStore(jdbc, new ObjectMapper());
        jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "unit-test-secret-only-0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwt, "expiration", 60000L);
        UserRoleService roles = mock(UserRoleService.class);
        sessions = new SessionService(store, jwt, new ReviewPermission(), roles);
        reviews = new ReviewService(mock(BusinessNotifications.class), store, mock(ReviewPermission.class),
                new ObjectMapper(), mock(OrderService.class));

        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FROM lin_profile_change")) {
                return List.of(Map.of("user_id", 7, "status", "pending", "requested_data", "{}"));
            }
            if (sql.contains("FROM lin_user")) {
                Map<String, Object> user = new HashMap<>();
                user.put("status", "normal"); user.put("review_status", null);
                user.put("credential_version", credentialVersion.get());
                user.put("hospital_id", 12); user.put("hospital_active", 1);
                return List.of(user);
            }
            return List.of();
        });
        when(jdbc.update(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.startsWith("UPDATE lin_doctor_profile SET credential_version")) {
                return 0;
            }
            if (sql.startsWith("INSERT INTO lin_doctor_profile")) {
                credentialVersion.incrementAndGet();
                return 1;
            }
            return 1;
        });
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(42, null, null, null), null, List.of()));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void legacyDoctorWithoutProfileGets401AfterProfileRejection() {
        String oldToken = jwt.generateToken(7, null, 12, 1, 0);
        assertTrue(sessions.valid(oldToken));

        reviews.profileChange(9L, new ReviewService.Decision(false, "资料不符"));

        assertFalse(sessions.valid(oldToken), "无 profile 旧医生被驳回后，旧 Token 必须返回 401");
    }
}
