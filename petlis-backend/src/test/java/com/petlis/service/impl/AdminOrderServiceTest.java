package com.petlis.service.impl;

import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminOrderServiceTest {
    @Test void orderListUsesLegacyIdsAndLaboratoryScope() {
        FeatureStore store = mock(FeatureStore.class); JdbcTemplate jdbc = mock(JdbcTemplate.class); AccessService access = mock(AccessService.class);
        when(store.jdbc()).thenReturn(jdbc);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.LAB_STAFF), Set.of(), Set.of(2), new DataScope(false, Set.of(), Set.of(2), null)));
        when(store.page(contains("lin_lb_listdiagnosisproject"), contains("UserNO"), eq(0), eq(20), any(Object[].class))).thenReturn(Map.of("items", List.of(Map.of("orderId", 8)), "total", 1, "page", 0, "size", 20));
        AdminOrderService service = new AdminOrderService(store, access);
        assertEquals(8, ((List<Map<String, Object>>) service.list(null, 0, 20).get("items")).get(0).get("orderId"));
        verify(store).page(anyString(), contains("lin_user_role"), eq(0), eq(20), any(Object[].class));
    }
}
