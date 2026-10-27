package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import com.petlis.dto.request.OrderCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderWorkflowServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    OrderWorkflowService service;

    @BeforeEach
    void setUp() {
        store = mock(FeatureStore.class);
        jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        access = mock(AccessService.class);
        when(access.current()).thenReturn(new CurrentActor(7, Set.of(PlatformRole.DOCTOR), Set.of(1), Set.of(),
                new DataScope(false, Set.of(1), Set.of(), null)));
        service = new OrderWorkflowService(store, access, new ObjectMapper());
    }

    OrderCreateRequest request(String key) {
        var request = new OrderCreateRequest();
        request.setIdempotencyKey(key);
        request.setAnimalName("豆豆");
        request.setMedicalHistory("既往病史");
        request.setClinicalSymptoms("咳嗽");
        request.setDiagnosis("疑似气管炎");
        request.setMedication("无");
        return request;
    }

    @Test
    void idempotentReplayReturnsOriginalOrderWithoutCreatingAnother() {
        when(store.one(contains("lin_order_idempotency"), eq(7), eq("ORDER_CREATE"), eq("same-key")))
                .thenReturn(Map.of("order_id", 8, "request_hash", service.requestHash(request("same-key")), "status", "COMPLETED"));
        assertEquals(OptionalInt.of(8), service.begin(request("same-key")));
        verify(store, never()).insert(contains("lin_lb_listdiagnosisproject"), any());
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejected() {
        when(store.one(contains("lin_order_idempotency"), eq(7), eq("ORDER_CREATE"), eq("same-key")))
                .thenReturn(Map.of("request_hash", "different", "status", "COMPLETED"));
        assertThrows(BusinessException.class, () -> service.begin(request("same-key")));
    }

    @Test
    void completionPersistsOrderAndClinicalSnapshotOnLegacyOrder() {
        service.complete(request("new-key"), 8, 1);
        verify(jdbc).update(contains("UPDATE lin_order_idempotency"), eq(8), any(), eq("COMPLETED"), eq(7), eq("new-key"));
        verify(jdbc).update(contains("INSERT INTO lin_order_clinical"), eq(8), isNull(), eq(1),
                eq("既往病史"), eq("咳嗽"), eq("疑似气管炎"), eq("无"), any(), eq(7));
    }
}
