package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OperationsCompensationTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    ReviewPermission permission;
    BusinessNotifications notifications;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class); permission = mock(ReviewPermission.class); notifications = mock(BusinessNotifications.class);
        when(store.jdbc()).thenReturn(jdbc); when(store.encode(any())).thenAnswer(call -> new ObjectMapper().writeValueAsString(call.getArgument(0)));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(5, null, 7, 1), null, List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void giftCancellationReturnsPointsAndStockExactlyOnceWithAudit() {
        MallService service = new MallService(notifications, store, permission, new ObjectMapper());
        when(store.one(contains("lin_redemption"), eq(31L))).thenReturn(Map.of("id", 31L, "user_id", 5, "gift_id", 9, "quantity", 2, "points_spent", 40, "status", "pending"));
        when(jdbc.queryForObject(contains("SELECT score"), eq(Integer.class), eq(5))).thenReturn(60);
        when(jdbc.update(contains("SET status='cancelled'"), eq(31L))).thenReturn(1);

        service.cancel(31, "用户取消");

        verify(jdbc).update(contains("lin_gift SET stock=stock+"), eq(2), eq(9L));
        verify(jdbc).update(contains("lin_user SET score=score+"), eq(40), eq(5));
        verify(jdbc).update(contains("lin_user_score_log"), eq(5), eq(40), eq(60), eq(100), anyString());
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("REDEMPTION_CANCELLED"), eq("REDEMPTION"), eq("31"), eq("用户取消"));
    }

    @Test void settledRedemptionCannotBeCancelledAgain() {
        MallService service = new MallService(notifications, store, permission, new ObjectMapper());
        when(store.one(contains("lin_redemption"), eq(31L))).thenReturn(Map.of("id", 31L, "user_id", 5, "gift_id", 9, "quantity", 2, "points_spent", 40, "status", "shipped"));
        assertThrows(BusinessException.class, () -> service.cancel(31, "重复取消"));
        verify(jdbc, never()).update(contains("stock=stock+"), any(), any());
    }

    @Test void pointsAdjustmentLocksUserAndRepeatsByRequestKey() {
        OperationalService service = new OperationalService(store, permission);
        when(jdbc.queryForList(contains("POINTS_ADJUSTMENT"), eq("points-key-1"))).thenReturn(List.of(Map.of("id", 1L)));
        service.adjustPoints(5, 20, "补偿", "points-key-1");
        verify(store, never()).one(anyString(), any());
        verify(jdbc, never()).update(contains("lin_user SET score"), any(), any());
    }

    @Test void pointsAdjustmentRechecksRequestKeyAfterUserLock() {
        OperationalService service = new OperationalService(store, permission);
        when(jdbc.queryForList(contains("POINTS_ADJUSTMENT"), eq("points-key-1"))).thenReturn(List.of(), List.of(Map.of("id", 1L)));
        when(store.one(contains("lin_user"), eq(5))).thenReturn(Map.of("score", 60));
        service.adjustPoints(5, 20, "补偿", "points-key-1");
        verify(jdbc, times(2)).queryForList(contains("POINTS_ADJUSTMENT"), eq("points-key-1"));
        verify(jdbc, never()).update(contains("lin_user SET score"), any(), any());
    }

    @Test void pointsRuleUsesVersionedOperationalConfigAndAudit() {
        OperationalService service = new OperationalService(store, permission);
        when(store.one(contains("lin_operational_config"), eq("points-rule"))).thenReturn(Map.of("version", 2, "config_value", "{\"register\":10}"));
        when(jdbc.update(contains("version=version+1"), any(Object[].class))).thenReturn(1);
        service.saveConfig("points-rule", Map.of("register", 20), 2, "规则调整");
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("OPERATIONAL_CONFIG_UPDATED"), eq("OPERATIONAL_CONFIG"), eq("points-rule"), eq("规则调整"));
    }
}
