package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContractCatalogReviewTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    ContractService service;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        service = new ContractService(store, mock(AccessService.class), mock(AuditService.class));
    }

    @Test void inactiveContractCatalogItemCannotBePricedForNewOrder() {
        when(jdbc.queryForList(contains("FROM lin_contract c"), eq(7), eq(2))).thenReturn(List.of(Map.of("id", 11L, "settlement_type", "MONTHLY")));
        when(store.one(contains("lin_lab_test"), eq(2), eq(91))).thenThrow(new BusinessException("项目已停用"));
        assertThrows(BusinessException.class, () -> service.priceForOrder(7, 2, List.of(91)));
        verify(jdbc, never()).queryForList(contains("lin_contract_item_price"), any(Object[].class));
    }

    @Test void contractPriceRejectsItemOutsideContractLaboratory() {
        when(store.one(contains("lin_contract"), eq(11L))).thenReturn(Map.of("id", 11L, "laboratory_id", 2));
        when(store.one(contains("lin_lab_test"), eq(2), eq(91))).thenThrow(new BusinessException("项目不属于实验室"));
        assertThrows(BusinessException.class, () -> service.savePrice(11, new ContractService.PriceCommand(91, BigDecimal.TEN, BigDecimal.TEN,
                null, LocalDate.now(), null, "ACTIVE")));
        verify(store, never()).insert(anyString(), any(Object[].class));
    }
}
