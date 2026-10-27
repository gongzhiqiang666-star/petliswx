package com.petlis.service.impl;

import com.petlis.common.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MonthlyBillSupplementTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    AuditService audits;
    MonthlyBillService service;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class); access = mock(AccessService.class); audits = mock(AuditService.class);
        when(store.jdbc()).thenReturn(jdbc);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.ADMIN), Set.of(), Set.of(), new DataScope(true, Set.of(), Set.of(), null)));
        service = new MonthlyBillService(store, access, audits, mock(BusinessNotifications.class));
    }

    @Test void lateOrderCreatesIndependentSupplementWithoutReopeningSettledBill() {
        snapshotContract();
        when(jdbc.queryForList(contains("FROM lin_monthly_bill WHERE contract_id"), any(Object[].class))).thenReturn(List.of(Map.of(
                "id", 21L, "status", "SETTLED", "paid_amount", new BigDecimal("100.00"), "supplement_no", 0)));
        unbilledOrder();
        when(store.insert(contains("lin_monthly_bill"), any(Object[].class))).thenReturn(22L);
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(22L))).thenReturn(List.of(Map.of("id", 22L, "hospital_id", 7, "laboratory_id", 2, "status", "PENDING_RECONCILIATION")));

        var result = service.generate(7, 2, "2026-09");

        assertEquals(22L, result.get("id"));
        verify(store).insert(contains("'SUPPLEMENT'"), any(Object[].class));
        verify(jdbc, never()).update(contains("lin_monthly_bill SET"), any(Object[].class));
        verify(jdbc, never()).update(contains("DELETE FROM lin_monthly_bill_item"), any(Object[].class));
    }

    @Test void historicalSnapshotContractStillBillsAfterContractBecameInactiveCash() {
        snapshotContract();
        when(jdbc.queryForList(contains("FROM lin_monthly_bill WHERE contract_id"), any(Object[].class))).thenReturn(List.of());
        unbilledOrder();
        when(store.insert(contains("lin_monthly_bill"), any(Object[].class))).thenReturn(22L);
        when(jdbc.queryForList(contains("FROM lin_monthly_bill b"), eq(22L))).thenReturn(List.of(Map.of("id", 22L, "hospital_id", 7, "laboratory_id", 2, "status", "PENDING_RECONCILIATION")));

        service.generate(7, 2, "2026-09");

        verify(store).one("SELECT id,billing_cycle_days FROM lin_contract WHERE id=? FOR UPDATE", 11L);
        verify(store, never()).one(contains("status='ACTIVE'"), any(), any());
        verify(jdbc).queryForList(contains("s.contract_id=?"), any(Object[].class));
    }

    private void snapshotContract() {
        when(jdbc.queryForList(contains("SELECT DISTINCT s.contract_id"), any(Object[].class))).thenReturn(List.of(Map.of("contract_id", 11L)));
        when(store.one("SELECT id,billing_cycle_days FROM lin_contract WHERE id=? FOR UPDATE", 11L)).thenReturn(Map.of("id", 11L, "billing_cycle_days", 30));
    }
    private void unbilledOrder() {
        when(jdbc.queryForList(contains("SELECT s.order_id,s.original_amount"), any(Object[].class))).thenReturn(List.of(Map.of(
                "order_id", 8, "payable_amount", new BigDecimal("90.00"), "original_amount", new BigDecimal("100.00"))));
        when(jdbc.queryForList(contains("lin_lb_diagnosisdetail d"), eq(8))).thenReturn(List.of(Map.of("detail_id", 81, "test_type_id", 91, "description", "血常规", "unit_price", new BigDecimal("100.00"))));
        when(jdbc.queryForList(contains("SELECT items_json FROM lin_order_pricing_snapshot"), eq(8))).thenReturn(List.of(Map.of("items_json", "[{\"testTypeId\":91,\"contractItemPriceId\":101,\"unitPrice\":100.00}]")));
        when(store.insert(contains("lin_monthly_bill_item"), any(Object[].class))).thenReturn(1L);
    }
}
