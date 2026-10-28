package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShipmentScanContractTest {
    @Test
    void scanReceiptRejectsBarcodeThatDoesNotMatchLegacyUserNo() {
        FeatureStore store = mock(FeatureStore.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        when(store.one(contains("UserNO"), eq(11L))).thenReturn(Map.of("id", 11L, "order_user_no", "55550-8"));
        var service = new ShipmentWorkflowService(store, mock(AccessService.class), mock(BusinessNotifications.class));
        assertThrows(BusinessException.class, () -> service.receiveScanned(11, "OTHER-BARCODE"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(store, never()).insert(anyString(), any(Object[].class));
    }
}
