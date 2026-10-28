package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserPrincipal;
import com.petlis.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewServiceSecurityTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    ReviewService service;

    @BeforeEach
    void setup() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        service = new ReviewService(mock(BusinessNotifications.class), store, mock(ReviewPermission.class),
                new ObjectMapper(), mock(OrderService.class));
    }

    private void login(int userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(userId, null, null, null), null, List.of()));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void legacyRegistrationEndpointRejectsSelfReview() {
        login(7);

        assertThrows(BusinessException.class, () -> service.registration(7, new ReviewService.Decision(true, "")));

        verify(store, never()).one(anyString(), any(Object[].class));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void legacyProfileChangeEndpointRejectsSelfReview() {
        login(7);
        when(store.one(contains("lin_profile_change"), eq(9L))).thenReturn(Map.of(
                "user_id", 7, "status", "pending", "requested_data", "{}"));

        assertThrows(BusinessException.class, () -> service.profileChange(9L, new ReviewService.Decision(false, "自审")));

        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void orderReviewUsesCashSnapshotInsteadOfCurrentMonthlyHospital() {
        login(42);
        when(store.one(contains("lin_order_pricing_snapshot"), eq(8))).thenReturn(Map.of(
                "SampleStatus", 0, "TestFee", 75.0, "CreateSampleId", 7, "PayType", 1, "settlement_type", "CASH"));

        service.order(8, new ReviewService.Decision(true, "现结合同"));

        verify(serviceOrders()).updateOrderStatus(8, -3);
    }

    @Test
    void orderReviewUsesMonthlySnapshotInsteadOfCurrentCashHospital() {
        login(42);
        when(store.one(contains("lin_order_pricing_snapshot"), eq(8))).thenReturn(Map.of(
                "SampleStatus", 0, "TestFee", 75.0, "CreateSampleId", 7, "PayType", 0, "settlement_type", "MONTHLY"));

        service.order(8, new ReviewService.Decision(true, "月结合同"));

        verify(serviceOrders()).updateOrderStatus(8, 2);
    }

    @Test
    void orderReviewWithoutSnapshotKeepsLegacyHospitalRule() {
        login(42);
        when(store.one(contains("lin_order_pricing_snapshot"), eq(8))).thenReturn(new java.util.HashMap<>(Map.of(
                "SampleStatus", 0, "TestFee", 75.0, "CreateSampleId", 7, "PayType", 1)));

        service.order(8, new ReviewService.Decision(true, "旧订单"));

        verify(serviceOrders()).updateOrderStatus(8, 2);
    }

    private OrderService serviceOrders() {
        return (OrderService) org.springframework.test.util.ReflectionTestUtils.getField(service, "orders");
    }

    @Test
    void rejectedProfileChangeRevokesCredentialVersion() {
        login(42);
        when(store.one(contains("lin_profile_change"), eq(9L))).thenReturn(Map.of(
                "user_id", 7, "status", "pending", "requested_data", "{}"));

        service.profileChange(9L, new ReviewService.Decision(false, "资料不符"));

        verify(jdbc).update("INSERT INTO lin_doctor_profile(user_id,review_status,credential_version) VALUES(?,'approved',1) ON DUPLICATE KEY UPDATE credential_version=credential_version+1", 7);
    }
}
