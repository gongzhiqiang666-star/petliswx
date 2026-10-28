package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MonthlyBillServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    AuditService audits;
    BusinessNotifications notifications;
    MonthlyBillService service;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class);
        access = mock(AccessService.class); audits = mock(AuditService.class); notifications = mock(BusinessNotifications.class);
        when(store.jdbc()).thenReturn(jdbc);
        lenient().when(jdbc.queryForList(contains("SELECT DISTINCT s.contract_id"), any(Object[].class))).thenReturn(List.of(Map.of("contract_id", 11L)));
        lenient().when(store.one("SELECT id,billing_cycle_days FROM lin_contract WHERE id=? FOR UPDATE", 11L)).thenReturn(Map.of("id", 11L, "billing_cycle_days", 30));
        service = new MonthlyBillService(store, access, audits, notifications);
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN));
    }

    CurrentActor actor(PlatformRole role) {
        return new CurrentActor(5, Set.of(role), Set.of(7), Set.of(2), new DataScope(role == PlatformRole.ADMIN, Set.of(7), Set.of(2), null));
    }

    @Test void generationUsesOrderSnapshotsWithoutCrossHospitalItems() {
        when(store.one(contains("FOR UPDATE"), eq(7), eq(2))).thenReturn(Map.of("id", 11L, "billing_cycle_days", 30));
        when(jdbc.queryForList(contains("FROM lin_monthly_bill WHERE contract_id"), any(Object[].class))).thenReturn(List.of());
        when(jdbc.queryForList(contains("SELECT s.order_id,s.original_amount"), any(Object[].class))).thenReturn(List.of(Map.of(
                "order_id", 8, "payable_amount", new BigDecimal("90.00"), "original_amount", new BigDecimal("100.00"))));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail d"), eq(8))).thenReturn(List.of(
                Map.of("detail_id", 81, "description", "血常规", "unit_price", new BigDecimal("60.00"), "contract_item_price_id", 101L),
                Map.of("detail_id", 82, "description", "生化", "unit_price", new BigDecimal("40.00"), "contract_item_price_id", 102L)));
        when(store.insert(contains("INSERT INTO lin_monthly_bill("), any(Object[].class))).thenReturn(21L);
        when(store.insert(contains("INSERT INTO lin_monthly_bill_item("), any(Object[].class))).thenReturn(1L, 2L);
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(List.of(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "PENDING_RECONCILIATION")));

        var bill = service.generate(7, 2, "2026-09");

        assertEquals(21L, bill.get("id"));
        verify(store).insert(contains("INSERT INTO lin_monthly_bill("), any(Object[].class));
        verify(store, times(2)).insert(contains("INSERT INTO lin_monthly_bill_item("), any(Object[].class));
    }

    @Test void zeroPricedMonthlyItemsDoNotDivideByZero() {
        when(store.one(contains("FOR UPDATE"), eq(7), eq(2))).thenReturn(Map.of("id", 11L, "billing_cycle_days", 30));
        when(jdbc.queryForList(contains("FROM lin_monthly_bill WHERE contract_id"), any(Object[].class))).thenReturn(List.of());
        when(jdbc.queryForList(contains("SELECT s.order_id,s.original_amount"), any(Object[].class))).thenReturn(List.of(Map.of(
                "order_id", 8, "payable_amount", BigDecimal.ZERO, "original_amount", BigDecimal.ZERO)));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail d"), eq(8))).thenReturn(List.of(Map.of("detail_id", 81, "description", "复查", "unit_price", BigDecimal.ZERO, "contract_item_price_id", 101L)));
        when(store.insert(contains("INSERT INTO lin_monthly_bill("), any(Object[].class))).thenReturn(21L);
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(List.of(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "PENDING_RECONCILIATION")));
        assertDoesNotThrow(() -> service.generate(7, 2, "2026-09"));
    }

    @Test void existingBillIsReturnedWithoutDuplicatingItems() {
        when(store.one(contains("FOR UPDATE"), eq(7), eq(2))).thenReturn(Map.of("id", 11L, "billing_cycle_days", 30));
        when(jdbc.queryForList(contains("FROM lin_monthly_bill WHERE contract_id"), any(Object[].class))).thenReturn(List.of(Map.of("id", 21L, "status", "PENDING_RECONCILIATION", "paid_amount", BigDecimal.ZERO)));
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(List.of(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "PENDING_RECONCILIATION")));
        service.generate(7, 2, "2026-09");
        verify(store, never()).insert(contains("INSERT INTO lin_monthly_bill("), any(Object[].class));
        verify(store, never()).insert(contains("INSERT INTO lin_monthly_bill_item("), any(Object[].class));
    }

    @Test void bothPartiesMustConfirmBeforeMockSettlement() {
        when(access.current()).thenReturn(actor(PlatformRole.HOSPITAL_ADMIN));
        when(store.one(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "PENDING_RECONCILIATION"));
        when(jdbc.queryForObject(contains("SELECT status"), eq(String.class), eq(21L))).thenReturn("PENDING_RECONCILIATION", "RECONCILED");
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(List.of(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "RECONCILED")));
        service.reconcile(21, "CONFIRM", null);
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN));
        service.reconcile(21, "CONFIRM", null);
        verify(jdbc).update(contains("hospital_reconciled_by"), eq(5), eq(21L));
        verify(jdbc).update(contains("laboratory_reconciled_by"), eq(5), eq(21L));
    }

    @Test void unconfirmedBillCannotSettleAndSettledReplayDoesNotReleaseTwice() {
        when(store.one(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "PENDING_RECONCILIATION"));
        when(jdbc.queryForObject(contains("SELECT status"), eq(String.class), eq(21L))).thenReturn("PENDING_RECONCILIATION");
        assertThrows(BusinessException.class, () -> service.settle(21, "mock-bill-21"));
        verify(jdbc, never()).update(contains("PayStatus2=1"), any(Object[].class));
    }

    @Test void reconciledBillMockSettlesOnceAndReplayDoesNotMarkDetailsAgain() {
        when(store.one(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "RECONCILED", "settlement_reference", "mock-bill-21"));
        when(jdbc.queryForObject(contains("SELECT status"), eq(String.class), eq(21L))).thenReturn("RECONCILED", "SETTLED");
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(List.of(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "SETTLED")));
        service.settle(21, "mock-bill-21");
        service.settle(21, "mock-bill-21");
        verify(jdbc, times(1)).update(contains("PayStatus2=1"), eq(21L));
        verify(jdbc, times(1)).update(contains("settlement_reference=?"), eq("mock-bill-21"), eq(21L));
    }

    @Test void doctorCannotReadHospitalBill() {
        when(access.current()).thenReturn(actor(PlatformRole.DOCTOR));
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(List.of(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2)));
        assertThrows(BusinessException.class, () -> service.get(21));
    }
}
