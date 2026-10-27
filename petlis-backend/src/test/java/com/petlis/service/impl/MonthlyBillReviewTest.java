package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MonthlyBillReviewTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    AuditService audits;
    BusinessNotifications notifications;
    MonthlyBillService service;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class); access = mock(AccessService.class); audits = mock(AuditService.class); notifications = mock(BusinessNotifications.class);
        when(store.jdbc()).thenReturn(jdbc);
        lenient().when(jdbc.queryForList(contains("SELECT DISTINCT s.contract_id"), any(Object[].class))).thenReturn(List.of(Map.of("contract_id", 11L)));
        lenient().when(store.one("SELECT id,billing_cycle_days FROM lin_contract WHERE id=? FOR UPDATE", 11L)).thenReturn(Map.of("id", 11L, "billing_cycle_days", 30));
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.ADMIN), Set.of(), Set.of(), new DataScope(true, Set.of(), Set.of(), null)));
        service = new MonthlyBillService(store, access, audits, notifications);
    }

    @Test void currentAndFuturePeriodsCannotGenerateBills() {
        assertThrows(BusinessException.class, () -> service.generate(7, 2, YearMonth.now().toString()));
        assertThrows(BusinessException.class, () -> service.generate(7, 2, YearMonth.now().plusMonths(1).toString()));
        verify(store, never()).one(anyString(), any(Object[].class));
    }

    @Test void newOrderAfterGenerationRecalculatesUnreconciledBillInsteadOfBeingOmitted() {
        existingBill("PENDING_RECONCILIATION");
        newOrderAfterGeneration();
        service.generate(7, 2, "2026-09");
        verify(jdbc).update(contains("DELETE FROM lin_monthly_bill_item"), eq(21L));
        verify(jdbc).update(contains("total_amount=?"), any(Object[].class));
        verify(store).insert(contains("lin_monthly_bill_item"), eq(21L), eq(8), eq(81), eq(101), eq("血常规"),
                eq(new BigDecimal("100.00")), eq(new BigDecimal("100.00")), eq(new BigDecimal("10.00")), eq(new BigDecimal("90.00")));
    }

    @Test void confirmedBillWithLateOrderCreatesIndependentSupplement() {
        existingBill("RECONCILED");
        newOrderAfterGeneration();
        when(store.insert(contains("lin_monthly_bill"), any(Object[].class))).thenReturn(22L);
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(22L))).thenReturn(List.of(Map.of("id", 22L, "hospital_id", 7, "laboratory_id", 2, "status", "PENDING_RECONCILIATION")));
        service.generate(7, 2, "2026-09");
        verify(jdbc, never()).update(contains("lin_monthly_bill SET"), any(Object[].class));
        verify(store).insert(contains("'SUPPLEMENT'"), any(Object[].class));
        verify(audits).record(eq("MONTHLY_BILL_SUPPLEMENT_GENERATED"), eq("MONTHLY_BILL"), eq(22L), any(), any(), any());
    }

    @Test void futureDueAndEmptyBillsCannotWriteOverdueReminder() {
        when(store.one(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "RECONCILED", "due_amount", new BigDecimal("10.00"), "paid_amount", BigDecimal.ZERO, "due_date", LocalDate.now().plusDays(1)));
        assertThrows(BusinessException.class, () -> service.remindOverdue(21));
        verify(jdbc, never()).update(contains("last_overdue_reminded_at"), any(Object[].class));

        when(store.one(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", "RECONCILED", "due_amount", BigDecimal.ZERO, "paid_amount", BigDecimal.ZERO, "due_date", LocalDate.now().minusDays(1)));
        when(jdbc.queryForObject(contains("MIN(order_id)"), eq(Integer.class), eq(21L))).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.remindOverdue(21));
        verify(jdbc, never()).update(contains("last_overdue_reminded_at"), any(Object[].class));
    }

    private void existingBill(String status) {
        when(store.one(contains("FOR UPDATE"), eq(7), eq(2))).thenReturn(Map.of("id", 11L, "billing_cycle_days", 30));
        when(jdbc.queryForList(contains("FROM lin_monthly_bill WHERE contract_id"), any(Object[].class))).thenReturn(List.of(Map.of("id", 21L, "status", status, "paid_amount", BigDecimal.ZERO, "supplement_no", 0)));
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(21L))).thenReturn(List.of(Map.of("id", 21L, "hospital_id", 7, "laboratory_id", 2, "status", status)));
    }
    private void newOrderAfterGeneration() {
        when(jdbc.queryForList(contains("SELECT s.order_id,s.original_amount"), any(Object[].class))).thenReturn(List.of(Map.of(
                "order_id", 8, "payable_amount", new BigDecimal("90.00"), "original_amount", new BigDecimal("100.00"))));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail d"), eq(8))).thenReturn(List.of(Map.of("detail_id", 81, "test_type_id", 91, "description", "血常规", "unit_price", new BigDecimal("100.00"))));
        when(jdbc.queryForList(contains("SELECT items_json FROM lin_order_pricing_snapshot"), eq(8))).thenReturn(List.of(Map.of("items_json", "[{\"testTypeId\":91,\"contractItemPriceId\":101,\"unitPrice\":100.00}]")));
        when(store.insert(contains("lin_monthly_bill_item"), any(Object[].class))).thenReturn(1L);
    }
}
