package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
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

    @Test void concurrentCreditReservationLocksContractAndRejectsOverflow() {
        when(store.one(contains("lin_contract"), eq(8))).thenReturn(Map.of("id", 11L, "credit_amount", new BigDecimal("100.00")));
        when(jdbc.queryForObject(contains("COALESCE(SUM(e.payable_amount),0)"), eq(BigDecimal.class), eq(11L), eq(8)))
                .thenReturn(new BigDecimal("60.00"));
        assertThrows(BusinessException.class, () -> service.reserveCredit(8, new BigDecimal("40.01")));
        verify(store).one(contains("FOR UPDATE"), eq(8));
    }

    @Test void creditBoundaryAllowsExactRemainingAmount() {
        when(store.one(contains("lin_contract"), eq(8))).thenReturn(Map.of("id", 11L, "credit_amount", new BigDecimal("100.00")));
        when(jdbc.queryForObject(contains("COALESCE(SUM(e.payable_amount),0)"), eq(BigDecimal.class), eq(11L), eq(8)))
                .thenReturn(new BigDecimal("60.00"));
        assertDoesNotThrow(() -> service.reserveCredit(8, new BigDecimal("40.00")));
    }
}
