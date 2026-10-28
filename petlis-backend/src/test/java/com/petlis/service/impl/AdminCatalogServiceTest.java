package com.petlis.service.impl;

import com.petlis.common.security.AccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminCatalogServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    AdminCatalogService service;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class); access = mock(AccessService.class);
        when(store.jdbc()).thenReturn(jdbc);
        service = new AdminCatalogService(store, access);
    }

    @Test void createUsesLegacyTestTypeAndLabMappingMasters() {
        when(store.insert(contains("lin_lb_infotesttype"), any(Object[].class))).thenReturn(91L);
        long id = service.createItem(2, new AdminCatalogService.ItemCommand("血常规", "CBC", new BigDecimal("88.50")));
        assertEquals(91, id);
        verify(access).requireLaboratory(2);
        verify(store).insert(contains("lin_lb_infotesttype"), any(Object[].class));
        verify(store).insert(contains("lin_lab_test"), eq(2), eq(91L), any());
    }

    @Test void updateChangesOnlyCurrentMastersWithoutTouchingOrderSnapshots() {
        service.updateItem(2, 91, new AdminCatalogService.ItemCommand("新名称", "NEW", new BigDecimal("99.00")));
        verify(jdbc).update(contains("UPDATE lin_lb_infotesttype"), any(Object[].class));
        verify(jdbc).update(contains("UPDATE lin_lab_test"), any(Object[].class));
        verify(jdbc, never()).update(contains("lin_order_pricing_snapshot"), any(Object[].class));
    }

    @Test void deleteLogicallyDeactivatesEvenWhenLegacyOrdersExist() {
        service.deleteItem(2, 91);
        verify(jdbc).update(contains("IsActive=0"), eq(91));
        verify(jdbc).update(contains("active=0"), eq(2), eq(91));
        verify(jdbc, never()).update(contains("DELETE FROM lin_lb_infotesttype"), any(Object[].class));
    }
}
