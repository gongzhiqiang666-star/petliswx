package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.DataScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContractServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    ContractService service;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class);
        jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        lenient().when(store.one(contains("lin_lab_test"), any(), any())).thenReturn(Map.of("test_type_id", 91));
        service = new ContractService(store, mock(AccessService.class), mock(AuditService.class));
    }

    @Test void agreedPriceHasPriorityAndDiscountUsesExactDecimalArithmetic() {
        when(jdbc.queryForList(contains("FROM lin_contract c"), eq(7), eq(2)))
                .thenReturn(List.of(Map.of("id", 11L, "settlement_type", "MONTHLY")));
        when(jdbc.queryForList(contains("lin_contract_item_price"), eq(11L), eq(2), eq(91))).thenReturn(List.of(Map.of(
                "id", 101L, "list_price", new BigDecimal("100.00"), "discount_rate", new BigDecimal("8.5000"), "agreed_price", new BigDecimal("12.34"))));
        var discounted = new java.util.HashMap<String, Object>();
        discounted.put("id", 102L); discounted.put("list_price", new BigDecimal("0.03"));
        discounted.put("discount_rate", new BigDecimal("8.5000")); discounted.put("agreed_price", null);
        when(jdbc.queryForList(contains("lin_contract_item_price"), eq(11L), eq(2), eq(92))).thenReturn(List.of(discounted));

        var result = service.priceForOrder(7, 2, List.of(91, 92));

        assertEquals("MONTHLY", result.settlementType());
        assertEquals(new BigDecimal("12.34"), result.items().get(91).finalPrice());
        assertEquals(new BigDecimal("0.03"), result.items().get(92).finalPrice());
    }

    @Test void activeContractRejectsMissingItemPrice() {
        when(jdbc.queryForList(contains("FROM lin_contract c"), eq(7), eq(2)))
                .thenReturn(List.of(Map.of("id", 11L, "settlement_type", "CASH")));
        when(jdbc.queryForList(contains("lin_contract_item_price"), eq(11L), eq(2), eq(91))).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.priceForOrder(7, 2, List.of(91)));
    }

    @Test void noActiveContractKeepsLegacyCashPricing() {
        when(jdbc.queryForList(contains("FROM lin_contract c"), eq(7), eq(2))).thenReturn(List.of());
        var result = service.priceForOrder(7, 2, List.of(91));
        assertEquals("CASH", result.settlementType());
        assertTrue(result.items().isEmpty());
    }

    @Test void createRejectsOverlappingActiveContractForSameParties() {
        when(jdbc.queryForObject(contains("c.status='ACTIVE'"), eq(Boolean.class), any(Object[].class))).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(command("OVERLAP")));
        verify(store, never()).insert(anyString(), any(Object[].class));
    }

    @Test void reactivateRejectsOverlapCreatedWhileContractWasInactive() {
        Map<String, Object> inactive = new java.util.HashMap<>();
        inactive.put("id", 11L); inactive.put("status", "INACTIVE"); inactive.put("contract_no", "A");
        inactive.put("hospital_id", 7); inactive.put("laboratory_id", 2); inactive.put("settlement_type", "CASH");
        inactive.put("billing_cycle_days", 30); inactive.put("credit_amount", BigDecimal.ZERO);
        inactive.put("effective_from", java.time.LocalDate.of(2026, 1, 1)); inactive.put("effective_to", java.time.LocalDate.of(2026, 12, 31));
        when(store.one(contains("lin_contract"), eq(11L))).thenReturn(inactive);
        when(jdbc.queryForObject(contains("c.status='ACTIVE'"), eq(Boolean.class), any(Object[].class))).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.setStatus(11L, true, "A停用期间已创建B"));
        verify(jdbc, never()).update(contains("status='ACTIVE'"), any(Object[].class));
        verify(jdbc).queryForObject(contains("c.id<>?"), eq(Boolean.class), eq(7), eq(2), any(), any(), any(), any(), eq(11L));
    }

    @Test void statusCanDeactivateAndReactivateWithoutChangingHistoricalContract() {
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("id", 11L); row.put("status", "ACTIVE"); row.put("contract_no", "A"); row.put("hospital_id", 7); row.put("laboratory_id", 2);
        row.put("settlement_type", "CASH"); row.put("billing_cycle_days", 30); row.put("credit_amount", BigDecimal.ZERO);
        row.put("effective_from", java.time.LocalDate.of(2026, 1, 1)); row.put("effective_to", java.time.LocalDate.of(2026, 12, 31));
        when(store.one(contains("SELECT * FROM lin_contract"), eq(11L))).thenReturn(row);
        AccessService access = (AccessService) org.springframework.test.util.ReflectionTestUtils.getField(service, "access");
        when(access.current()).thenReturn(new CurrentActor(5, java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), new DataScope(true, java.util.Set.of(), java.util.Set.of(), null)));
        service.setStatus(11L, false, "暂停合作");
        verify(jdbc).update(contains("status='INACTIVE'"), eq(5), eq(11L));
        service.setStatus(11L, true, "恢复合作");
        verify(jdbc).update(contains("status='ACTIVE'"), eq(5), eq(11L));
    }

    private ContractService.Command command(String number) {
        return new ContractService.Command(number, 7, 2, "CASH", 30, BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 12, 31));
    }

    @Test void inactiveContractCannotFallBackToLegacyPricingForNewOrders() {
        when(jdbc.queryForList(contains("FROM lin_contract c"), eq(7), eq(2))).thenReturn(List.of());
        when(jdbc.queryForObject(contains("c.status='INACTIVE'"), eq(Boolean.class), eq(7), eq(2))).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.priceForOrder(7, 2, List.of(91)));
    }

    @Test void concurrentCreditReservationLocksContractAndRejectsOverflow() {
        when(store.one(contains("lin_contract"), eq(8))).thenReturn(Map.of("id", 11L, "credit_amount", new BigDecimal("100.00")));
        when(jdbc.queryForObject(contains("COALESCE(SUM(s.payable_amount),0)"), eq(BigDecimal.class), eq(11L), eq(8)))
                .thenReturn(new BigDecimal("60.00"));
        assertThrows(BusinessException.class, () -> service.reserveCredit(8, new BigDecimal("40.01")));
        verify(store).one(contains("FOR UPDATE"), eq(8));
    }

    @Test void creditBoundaryAllowsExactRemainingAmount() {
        when(store.one(contains("lin_contract"), eq(8))).thenReturn(Map.of("id", 11L, "credit_amount", new BigDecimal("100.00")));
        when(jdbc.queryForObject(contains("COALESCE(SUM(s.payable_amount),0)"), eq(BigDecimal.class), eq(11L), eq(8)))
                .thenReturn(new BigDecimal("60.00"));
        assertDoesNotThrow(() -> service.reserveCredit(8, new BigDecimal("40.00")));
    }
}
