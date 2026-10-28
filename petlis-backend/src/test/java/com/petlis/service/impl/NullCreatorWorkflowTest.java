package com.petlis.service.impl;

import com.petlis.common.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NullCreatorWorkflowTest {
    @Test
    void logisticsCreationWithNullCreatorFallsBackWithoutNpe() {
        FeatureStore store = mock(FeatureStore.class);
        AccessService access = mock(AccessService.class);
        BusinessNotifications notifications = mock(BusinessNotifications.class);
        when(access.current()).thenReturn(new CurrentActor(9, Set.of(PlatformRole.ADMIN), Set.of(), Set.of(), new DataScope(true, Set.of(), Set.of(), null)));
        Map<String, Object> order = new HashMap<>();
        order.put("order_id", 8); order.put("hospital_id", 1); order.put("laboratory_id", 2); order.put("doctor_user_id", null); order.put("review_status", "approved");
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(order);
        when(store.insert(contains("lin_shipment"), any(Object[].class))).thenReturn(11L);
        var service = new ShipmentWorkflowService(store, access, notifications);
        assertDoesNotThrow(() -> service.create(8, new ShipmentWorkflowService.ShipmentCommand("SF-8", 2, "前台", null, 6)));
        verify(notifications).emitForOrder(eq(8), isNull(), eq("order"), eq("shipment:11:PENDING_PICKUP"), any(), any(), any());
    }

    @Test
    void laboratoryTransitionWithNullCreatorFallsBackWithoutNpe() {
        FeatureStore store = mock(FeatureStore.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        AccessService access = mock(AccessService.class);
        BusinessNotifications notifications = mock(BusinessNotifications.class);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.LAB_STAFF), Set.of(), Set.of(2), new DataScope(false, Set.of(), Set.of(2), null)));
        Map<String, Object> task = new HashMap<>();
        task.put("id", 11L); task.put("order_id", 8); task.put("laboratory_id", 2); task.put("status", "TESTING"); task.put("doctor_user_id", null);
        when(store.one(contains("lin_lab_task"), eq(11L))).thenReturn(task);
        var service = new LabWorkflowService(store, access, notifications);
        assertDoesNotThrow(() -> service.returnSample(11, "退样"));
        verify(notifications).emitForOrder(eq(8), isNull(), eq("order"), any(), any(), any(), any());
    }

    @Test
    void reportPublishWithNullCreatorFallsBackWithoutNpe() {
        FeatureStore store = mock(FeatureStore.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        AccessService access = mock(AccessService.class);
        BusinessNotifications notifications = mock(BusinessNotifications.class);
        when(access.current()).thenReturn(new CurrentActor(5, Set.of(PlatformRole.LAB_STAFF), Set.of(), Set.of(2), new DataScope(false, Set.of(), Set.of(2), null)));
        Map<String, Object> report = new HashMap<>();
        report.put("id", 41L); report.put("order_id", 8); report.put("detail_id", 21); report.put("test_result_id", 31L);
        report.put("hospital_id", 1); report.put("laboratory_id", 2); report.put("status", "APPROVED"); report.put("doctor_user_id", null);
        report.put("is_pay_money", 1); report.put("pay_type", 0); report.put("pdf_file_id", 51L); report.put("batch_no", "B-1");
        when(store.one(contains("SELECT w.*"), eq(41L))).thenReturn(report);
        when(store.one(contains("COUNT(DISTINCT"), eq(8))).thenReturn(Map.of("total", 1, "published", 1));
        when(store.one(contains("w.order_id,w.test_result_id"), eq(41L))).thenReturn(Map.of("order_id", 8, "test_result_id", 31L));
        when(store.one(contains("SampleStatus"), eq(41L))).thenReturn(Map.of("order_id", 8, "test_result_id", 31L, "SampleStatus", 2));
        when(store.one(startsWith("SELECT t.id,t.status"), eq(31L))).thenReturn(Map.of("status", "REPORT_REVIEW"));
        var service = new ReportWorkflowService(store, access, mock(LabWorkflowService.class), notifications, mock(ReportPdfGenerator.class), mock(PlatformFileService.class));
        assertDoesNotThrow(() -> service.publish(41));
        verify(notifications).emitForOrder(eq(8), isNull(), eq("report"), eq("8:published"), any(), any(), any());
    }
}
