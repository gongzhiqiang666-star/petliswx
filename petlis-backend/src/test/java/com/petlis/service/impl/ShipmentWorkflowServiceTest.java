package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.DataScope;
import com.petlis.common.security.PlatformRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShipmentWorkflowServiceTest {
    FeatureStore store;
    JdbcTemplate jdbc;
    AccessService access;
    BusinessNotifications notifications;
    ShipmentWorkflowService service;

    @BeforeEach
    void setUp() {
        store = mock(FeatureStore.class);
        jdbc = mock(JdbcTemplate.class);
        when(store.jdbc()).thenReturn(jdbc);
        access = mock(AccessService.class);
        notifications = mock(BusinessNotifications.class);
        service = new ShipmentWorkflowService(store, access, notifications);
    }

    CurrentActor actor(PlatformRole role, int userId) {
        return new CurrentActor(userId, Set.of(role), Set.of(1), Set.of(2),
                new DataScope(role == PlatformRole.ADMIN, Set.of(1), Set.of(2),
                        role == PlatformRole.COURIER ? userId : null));
    }

    @Test
    void adminCreatesPendingShipmentAndEmitsOneBusinessEvent() {
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN, 9));
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(Map.of(
                "order_id", 8, "hospital_id", 1, "laboratory_id", 2, "doctor_user_id", 7, "review_status", "approved"));
        when(store.insert(contains("lin_shipment"), any(Object[].class))).thenReturn(11L);

        var result = service.create(8, new ShipmentWorkflowService.ShipmentCommand(
                "SF-8", 2, "医院前台", "2026-10-28T10:00:00", 6));

        assertEquals(11L, result);
        verify(notifications).emitForOrder(eq(8), eq(7), eq("order"), eq("shipment:11:PENDING_PICKUP"), any(), any(), any());
    }

    @Test
    void courierCannotOperateAnotherCouriersShipment() {
        when(access.current()).thenReturn(actor(PlatformRole.COURIER, 5));
        when(store.one(contains("lin_shipment"), eq(11L))).thenReturn(Map.of(
                "id", 11L, "order_id", 8, "hospital_id", 1, "laboratory_id", 2,
                "courier_user_id", 6, "status", "PENDING_PICKUP", "doctor_user_id", 7));
        doThrow(new BusinessException(403, "越权")).when(access).requireCourierAssignment(6);

        assertThrows(BusinessException.class, () -> service.transition(11,
                ShipmentWorkflowService.ShipmentAction.PICKUP, null, null));
        verify(jdbc, never()).update(contains("UPDATE lin_shipment"), any(Object[].class));
    }

    @Test
    void laboratoryReceiptCreatesInitialTaskInSameTransaction() {
        when(access.current()).thenReturn(actor(PlatformRole.LAB_STAFF, 5));
        when(store.one(contains("lin_shipment"), eq(11L))).thenReturn(Map.of(
                "id", 11L, "order_id", 8, "hospital_id", 1, "laboratory_id", 2,
                "courier_user_id", 6, "status", "DELIVERED", "doctor_user_id", 7));

        service.transition(11, ShipmentWorkflowService.ShipmentAction.RECEIVE, null, null);

        verify(store).insert(contains("lin_lab_task"), any(Object[].class));
        verify(notifications).emitForOrder(eq(8), eq(7), eq("order"), eq("shipment:11:RECEIVED"), any(), any(), any());
    }

    @Test
    void duplicateReceiptReturnsOriginalWithoutTaskTimelineOrNotification() {
        when(access.current()).thenReturn(actor(PlatformRole.LAB_STAFF, 5));
        when(store.one(contains("lin_shipment"), eq(11L))).thenReturn(Map.of(
                "id", 11L, "order_id", 8, "hospital_id", 1, "laboratory_id", 2,
                "courier_user_id", 6, "status", "RECEIVED", "doctor_user_id", 7));
        assertDoesNotThrow(() -> service.transition(11, ShipmentWorkflowService.ShipmentAction.RECEIVE, null, null));
        verify(store, never()).insert(anyString(), any(Object[].class));
        verifyNoInteractions(notifications);
    }

    @Test
    void unapprovedOrderCannotCreateShipment() {
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN, 9));
        when(store.one(contains("lin_lb_listdiagnosisproject"), eq(8))).thenReturn(Map.of(
                "order_id", 8, "hospital_id", 1, "laboratory_id", 2, "doctor_user_id", 7, "review_status", "pending"));
        assertThrows(BusinessException.class, () -> service.create(8, new ShipmentWorkflowService.ShipmentCommand("SF-8", 2, "前台", null, 6)));
        verify(store, never()).insert(contains("lin_shipment"), any(Object[].class));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING_PICKUP,PICKUP,PICKED_UP",
            "PICKED_UP,TRANSIT,IN_TRANSIT",
            "IN_TRANSIT,DELIVER,DELIVERED",
            "DELIVERED,RECEIVE,RECEIVED"
    })
    void onlyTheDocumentedShipmentTransitionIsAccepted(String from, ShipmentWorkflowService.ShipmentAction action, String target) {
        assertEquals(target, service.nextStatus(from, action));
        for (String other : List.of("PENDING_PICKUP", "PICKED_UP", "IN_TRANSIT", "DELIVERED", "RECEIVED")) {
            if (!other.equals(from)) assertThrows(BusinessException.class, () -> service.nextStatus(other, action));
        }
    }

    @Test
    void exceptionRequiresReasonAndDoesNotDestroyCurrentState() {
        when(access.current()).thenReturn(actor(PlatformRole.COURIER, 6));
        when(store.one(contains("lin_shipment"), eq(11L))).thenReturn(Map.of(
                "id", 11L, "order_id", 8, "hospital_id", 1, "laboratory_id", 2,
                "courier_user_id", 6, "status", "IN_TRANSIT", "doctor_user_id", 7));
        assertThrows(BusinessException.class, () -> service.transition(11,
                ShipmentWorkflowService.ShipmentAction.EXCEPTION, "DELAY", " "));
        verify(jdbc, never()).update(contains("exception_type"), any(Object[].class));
    }
}
