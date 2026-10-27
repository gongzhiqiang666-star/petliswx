package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.DataScope;
import com.petlis.common.security.PlatformRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PetHealthServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    PetHealthService service;

    @BeforeEach
    void setUp() {
        store = mock(FeatureStore.class);
        jdbc = mock(JdbcTemplate.class);
        access = mock(AccessService.class);
        when(store.jdbc()).thenReturn(jdbc);
        service = new PetHealthService(store, access);
        when(access.current()).thenReturn(new CurrentActor(9, Set.of(PlatformRole.DOCTOR), Set.of(1), Set.of(),
                new DataScope(false, Set.of(1), Set.of(), null)));
    }

    @Test
    void healthCombinesClinicalHistoryAndLegacyReportsWithoutChangingIds() {
        when(store.one(contains("lin_lb_pet"), eq(7))).thenReturn(Map.of("id", 7, "hospital_id", 1));
        when(jdbc.queryForList(contains("lin_order_clinical"), eq(7), eq(1))).thenReturn(List.of(
                Map.of("orderId", 11, "medicalHistory", "慢性鼻炎", "diagnosis", "复查", "createDt", "2026-10-01")));
        when(store.page(contains("lin_monthly_bill"), anyString(), anyInt(), anyInt(), any())).thenThrow(new AssertionError("错误主表"));
        when(store.page(contains("lin_lb_diagnosisdetail"), contains("lin_lb_diagnosisdetail"), eq(0), eq(20), eq(7), eq(1)))
                .thenReturn(Map.of("items", List.of(Map.of("reportId", 31, "orderId", 11)), "total", 1, "page", 0, "size", 20));

        Map<String, Object> result = service.health(7, 0, 20);

        assertEquals(7, result.get("petId"));
        assertEquals("慢性鼻炎", ((List<Map<String, Object>>) result.get("medicalHistory")).get(0).get("medicalHistory"));
        assertEquals(31, ((Map<String, Object>) ((List<?>) ((Map<?, ?>) result.get("reports")).get("items")).get(0)).get("reportId"));
    }

    @Test
    void healthRejectsPetOutsideCurrentHospital() {
        when(store.one(contains("lin_lb_pet"), eq(7))).thenReturn(Map.of("id", 7, "hospital_id", 2));
        doThrow(new BusinessException(403, "越权")).when(access).requireHospital(2);
        assertThrows(BusinessException.class, () -> service.health(7, 0, 20));
        verifyNoInteractions(jdbc);
    }
}
