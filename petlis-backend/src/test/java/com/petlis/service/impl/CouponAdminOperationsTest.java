package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CouponAdminOperationsTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    CouponService service;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc); when(store.encode(any())).thenReturn("{}");
        service = new CouponService(store, mock(ReviewPermission.class), mock(ContractService.class));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(5, null, 7, 1), null, List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void grantLocksTemplateStockAndWritesAudit() {
        when(jdbc.queryForList(contains("COUPON_GRANT"), eq("grant-key-1"))).thenReturn(List.of());
        when(store.one(contains("lin_user"), eq(5))).thenReturn(Map.of("id", 5));
        when(store.one(contains("lin_coupon"), eq(9L))).thenReturn(Map.of("id", 9L));
        when(store.one(contains("lin_operational_config"), eq("coupon-stock:9"))).thenReturn(new java.util.HashMap<>(Map.of("config_value", "{\"total\":2,\"issued\":1}")));
        when(store.insert(contains("lin_user_coupon"), eq(5), eq(9L))).thenReturn(31L);

        service.grant(9, 5, "grant-key-1");

        verify(jdbc).update(contains("lin_operational_config SET config_value"), eq("{\"total\":2,\"issued\":2}"), eq(5), eq("coupon-stock:9"));
        verify(jdbc).update(contains("lin_audit_record"), eq(5), eq("COUPON_GRANTED"), eq("USER_COUPON"), eq("31"), eq("grant-key-1"));
    }

    @Test void exhaustedStockRejectsConcurrentGrantAfterLockedRead() {
        when(jdbc.queryForList(contains("COUPON_GRANT"), eq("grant-key-1"))).thenReturn(List.of());
        when(store.one(contains("lin_user"), eq(5))).thenReturn(Map.of("id", 5));
        when(store.one(contains("lin_coupon"), eq(9L))).thenReturn(Map.of("id", 9L));
        when(store.one(contains("lin_operational_config"), eq("coupon-stock:9"))).thenReturn(new java.util.HashMap<>(Map.of("config_value", "{\"total\":1,\"issued\":1}")));
        assertThrows(BusinessException.class, () -> service.grant(9, 5, "grant-key-1"));
        verify(store, never()).insert(contains("lin_user_coupon"), any(), any());
    }

    @Test void repeatedGrantDoesNotConsumeStockAgain() {
        when(jdbc.queryForList(contains("COUPON_GRANT"), eq("grant-key-1"))).thenReturn(List.of(Map.of("resource_id", "31")));
        assertEquals(31, service.grant(9, 5, "grant-key-1"));
        verify(store, never()).insert(anyString(), any(Object[].class));
    }

    @Test void grantRechecksRequestKeyAfterUserLock() {
        when(jdbc.queryForList(contains("COUPON_GRANT"), eq("grant-key-1"))).thenReturn(List.of(), List.of(Map.of("resource_id", "31")));
        when(store.one(contains("lin_user"), eq(5))).thenReturn(Map.of("id", 5));
        assertEquals(31, service.grant(9, 5, "grant-key-1"));
        verify(store, never()).insert(anyString(), any(Object[].class));
        verify(store, never()).one(contains("lin_coupon"), any());
    }

    @Test void stockUpdateNeverResetsIssuedQuantity() {
        CouponService.Coupon coupon = new CouponService.Coupon("复查券", new BigDecimal("10.00"), BigDecimal.ZERO, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        when(jdbc.queryForList(contains("lin_operational_config"), eq("coupon-stock:9"))).thenReturn(List.of(Map.of("config_value", "{\"total\":2,\"issued\":2}")));
        service.updateCoupon(9, coupon, 3, "调整库存");
        verify(jdbc).update(contains("coupon-stock:9"), eq("{\"total\":3,\"issued\":2}"), eq(5));
    }

    @Test void templateCrudRetainsOldCouponMasterAndAudits() {
        CouponService.Coupon coupon = new CouponService.Coupon("复查券", new BigDecimal("10.00"), BigDecimal.ZERO, null,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        service.updateCoupon(9, coupon, 3, "调整库存");
        service.deactivateCoupon(9);
        verify(jdbc).update(contains("UPDATE lin_coupon SET name="), any(Object[].class));
        verify(jdbc).update(contains("coupon-stock:9"), any(Object[].class));
        verify(jdbc).update(contains("UPDATE lin_coupon SET active=0"), eq(9L));
        verify(jdbc, atLeast(2)).update(contains("lin_audit_record"), any(Object[].class));
    }
}
