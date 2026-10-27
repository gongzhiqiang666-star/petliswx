package com.petlis.service.impl;

import com.petlis.common.security.AccessService;
import com.petlis.common.security.ReviewPermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminOperationalSupportTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    ReviewPermission permission;

    @BeforeEach void setUp() {
        store = mock(FeatureStore.class); jdbc = mock(JdbcTemplate.class); permission = mock(ReviewPermission.class);
        when(store.jdbc()).thenReturn(jdbc);
    }

    @Test void catalogReadsExistingLaboratoryAndLabTestMasters() {
        AdminCatalogService service = new AdminCatalogService(store, mock(AccessService.class));
        when(store.page(contains("lin_laboratory"), contains("lin_laboratory"), eq(0), eq(20), any(Object[].class))).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        when(store.page(contains("lin_lab_test"), contains("lin_lb_infotesttype"), eq(0), eq(20), any(Object[].class))).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        assertEquals(0, service.laboratories(0, 20).get("page"));
        assertEquals(0, service.items(2, 0, 20).get("page"));
        verify(store).page(contains("lin_laboratory"), contains("description"), eq(0), eq(20), any(Object[].class));
        service.setItemStatus(2, 91, false);
        verify(jdbc).update("UPDATE lin_lab_test SET active=? WHERE laboratory_id=? AND test_type_id=?", 0, 2, 91);
    }

    @Test void adminGiftAndRedemptionListsKeepExistingMasters() {
        MallService service = new MallService(mock(BusinessNotifications.class), store, permission, new ObjectMapper());
        when(store.page(contains("lin_gift"), contains("lin_gift"), eq(0), eq(20), any(Object[].class))).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        when(store.page(contains("lin_redemption"), contains("lin_redemption"), eq(0), eq(20), any(Object[].class))).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        service.adminGifts(0, 20);
        service.adminRedemptions(0, 20);
        verify(permission, times(2)).requireReviewer();
    }

    @Test void auditListAndDetailUsePlatformAuditTable() {
        OperationalService service = new OperationalService(store, permission);
        when(store.page(contains("lin_audit_record"), contains("lin_audit_record"), eq(0), eq(20), any(Object[].class))).thenReturn(Map.of("items", List.of(), "total", 0, "page", 0, "size", 20));
        when(store.one(contains("lin_audit_record"), eq(8L))).thenReturn(Map.of("id", 8L));
        service.audits(null, 0, 20);
        service.audit(8);
        verify(permission, times(2)).requireReviewer();
    }
}
