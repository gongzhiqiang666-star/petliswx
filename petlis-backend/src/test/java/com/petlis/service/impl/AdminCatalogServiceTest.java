package com.petlis.service.impl;

import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.DataScope;
import com.petlis.common.security.PlatformRole;
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

    @Test void adminCanUpdateGlobalDictionaryAndMappingWithoutTouchingOrderSnapshots() {
        actor(PlatformRole.ADMIN);
        service.updateItem(2, 91, new AdminCatalogService.ItemCommand("新名称", "NEW", new BigDecimal("99.00")));
        verify(jdbc).update(contains("UPDATE lin_lb_infotesttype"), any(Object[].class));
        verify(jdbc).update(contains("UPDATE lin_lab_test"), any(Object[].class));
        verify(jdbc, never()).update(contains("lin_order_pricing_snapshot"), any(Object[].class));
    }

    @Test void labStaffUpdateAndDeleteOnlyTouchOwnMapping() {
        actor(PlatformRole.LAB_STAFF);
        service.updateItem(2, 91, new AdminCatalogService.ItemCommand("新名称", "NEW", new BigDecimal("99.00")));
        service.deleteItem(2, 91);
        service.setItemStatus(2, 91, false);
        verify(jdbc, never()).update(contains("UPDATE lin_lb_infotesttype"), any(Object[].class));
        verify(jdbc).update(contains("UPDATE lin_lab_test SET price=?"), any(Object[].class));
        verify(jdbc).update("UPDATE lin_lab_test SET active=0 WHERE laboratory_id=? AND test_type_id=?", 2, 91);
        verify(jdbc).update("UPDATE lin_lab_test SET active=? WHERE laboratory_id=? AND test_type_id=?", 0, 2, 91);
    }

    private void actor(PlatformRole role) {
        when(access.current()).thenReturn(new CurrentActor(5, java.util.Set.of(role), java.util.Set.of(), java.util.Set.of(2),
                new DataScope(role == PlatformRole.ADMIN, java.util.Set.of(), java.util.Set.of(2), null)));
    }
}
