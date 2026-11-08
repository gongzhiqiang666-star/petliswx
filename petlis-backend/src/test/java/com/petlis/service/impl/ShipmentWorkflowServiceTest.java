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
                "order_id", 8, "hospital_id", 1, "laboratory_id", 2, "doctor_user_id", 7, "review_status", "approved", "order_status", 2));
        when(store.insert(contains("lin_shipment"), any(Object[].class))).thenReturn(11L);

        var result = service.create(8, new ShipmentWorkflowService.ShipmentCommand(
                "SF-8", 2, "医院前台", "2026-10-28T10:00:00", 6));

        assertEquals(11L, result);
        verify(notifications).emitForOrder(eq(8), eq(7), eq("order"), eq("shipment:11:PENDING_PICKUP"), any(), any(), any());
    }

    @Test
    void pendingCarrierRequestPreventsManualDuplicateShipment() {
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN,9));
        when(store.one(contains("lin_lb_listdiagnosisproject"),eq(8))).thenReturn(Map.of(
            "order_id",8,"hospital_id",1,"laboratory_id",2,"review_status","approved","order_status",2));
        when(jdbc.queryForList(contains("lin_sf_request"),eq(8))).thenReturn(List.of(Map.of("state","UNKNOWN")));
        assertThrows(BusinessException.class,()->service.create(8,new ShipmentWorkflowService.ShipmentCommand("MANUAL",2,"前台",null,null)));
        verify(store,never()).insert(contains("lin_shipment"),any(Object[].class));
    }

    @Test
    void unconfirmedCancellationCannotChangeShipment() {
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN,9));
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("order_id",8,"status","PENDING_PICKUP"));
        assertThrows(BusinessException.class,()->service.applySfCancellation(11));
        verify(jdbc,never()).update(contains("UPDATE lin_shipment"),any(Object[].class));
        verifyNoInteractions(notifications);
    }
    @Test
    void confirmedCancellationUpdatesOnlyShipmentAndEmitsOneEvent() {
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN,9));
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("id",11L,"order_id",8,"attempt_no",1,"status","PENDING_PICKUP","doctor_user_id",7));
        when(jdbc.queryForList(contains("operation='CANCEL'"),eq(8),eq(1))).thenReturn(List.of(Map.of("id",1)));
        service.applySfCancellation(11);
        verify(jdbc).update(contains("SET status='CANCELLED'"),eq(11L));
        verify(jdbc,never()).update(contains("UPDATE lin_lb_listdiagnosisproject"),any(Object[].class));
        verify(notifications).emitForOrder(eq(8),eq(7),eq("order"),eq("shipment:11:CANCELLED"),any(),any(),any());
    }
    @Test
    void pendingCancellationBlocksCourierPickup() {
        when(access.current()).thenReturn(actor(PlatformRole.COURIER,6));
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("id",11L,"order_id",8,"attempt_no",1,"courier_user_id",6,"status","PENDING_PICKUP"));
        when(jdbc.queryForList(contains("operation='CANCEL'"),eq(8),eq(1))).thenReturn(List.of(Map.of("state","UNKNOWN")));
        assertThrows(BusinessException.class,()->service.transition(11,ShipmentWorkflowService.ShipmentAction.PICKUP,null,null));
        verify(jdbc,never()).update(contains("UPDATE lin_shipment"),any(Object[].class));
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
    @Test void sameExceptionRequestIsReadOnlyAndChangedPayloadConflicts() {
        when(access.current()).thenReturn(actor(PlatformRole.COURIER,6));
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("id",11L,"order_id",8,"hospital_id",1,"laboratory_id",2,"courier_user_id",6,"status","IN_TRANSIT","doctor_user_id",7));
        when(jdbc.queryForList(contains("request_id=?"),eq("11"),eq("exception_request_123"))).thenReturn(List.of(Map.of("after_json","{\"exceptionType\":\"DAMAGE\",\"note\":\"试管破损\"}")));
        service.transition(11,ShipmentWorkflowService.ShipmentAction.EXCEPTION,"DAMAGE","试管破损","exception_request_123");
        assertThrows(BusinessException.class,()->service.transition(11,ShipmentWorkflowService.ShipmentAction.EXCEPTION,"LOSS","试管破损","exception_request_123"));
        verify(jdbc,never()).update(contains("exception_type"),any(Object[].class));
        verify(store,never()).insert(anyString(),any(Object[].class));
        verifyNoInteractions(notifications);
    }
    @Test void differentIncidentsHaveSeparateNotificationKeysAndReadableReasons() throws Exception {
        when(access.current()).thenReturn(actor(PlatformRole.COURIER,6));
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("id",11L,"order_id",8,"hospital_id",1,"laboratory_id",2,"courier_user_id",6,"status","IN_TRANSIT","doctor_user_id",7));
        when(store.encode(any())).thenAnswer(call->new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(call.getArgument(0)));
        when(store.insert(contains("request_id,after_json"),any(Object[].class))).thenReturn(71L,72L);
        service.transition(11,ShipmentWorkflowService.ShipmentAction.EXCEPTION,"DAMAGE","试管破损","exception_request_123");
        service.transition(11,ShipmentWorkflowService.ShipmentAction.EXCEPTION,"LOSS","包裹丢失","exception_request_456");
        verify(notifications).emitForOrder(eq(8),eq(7),eq("order"),eq("shipment:11:exception:71"),eq("标本物流异常"),eq("标本破损：试管破损"),anyString());
        verify(notifications).emitForOrder(eq(8),eq(7),eq("order"),eq("shipment:11:exception:72"),eq("标本物流异常"),eq("标本丢失：包裹丢失"),anyString());
    }
    @Test void legacyClientUsesBoundedStableKeyWithoutRequiringSchemaChanges() {
        when(access.current()).thenReturn(actor(PlatformRole.COURIER,6));
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("id",11L,"order_id",8,"hospital_id",1,"laboratory_id",2,"courier_user_id",6,"status","IN_TRANSIT","doctor_user_id",7));
        service.transition(11,ShipmentWorkflowService.ShipmentAction.EXCEPTION,"OTHER","原客户端异常");
        verify(store).insert(contains("request_id,after_json"),eq(6),eq("11"),argThat((String key)->key.matches("[0-9a-f]{64}")),nullable(String.class),eq("原客户端异常"));
    }
    @Test void fullLengthExceptionNoteHasSafeNotificationSummaryWithoutSplittingUnicode() {
        String note="标".repeat(994)+"🐈🐈🐈";
        assertEquals(1000,note.length());
        String message=ShipmentWorkflowService.exceptionMessage("DELAY",note);
        assertTrue(message.length()<=1000);
        assertTrue(message.endsWith("…"));
        assertFalse(Character.isHighSurrogate(message.charAt(message.length()-2)));
    }
    @Test void resolvingLastIncidentClearsCurrentWarningAndCreatesOneResolutionEvent() {
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN,9));
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("id",11L,"order_id",8,"hospital_id",1,"laboratory_id",2,"courier_user_id",6,"status","IN_TRANSIT","doctor_user_id",7));
        when(store.one(contains("action='EXCEPTION' FOR UPDATE"),eq(71L),eq("11"))).thenReturn(Map.of("id",71L));
        when(store.insert(contains("'EXCEPTION_RESOLVED'"),any(Object[].class))).thenReturn(91L);
        service.resolveException(11,71,"已核对取回标本");
        verify(jdbc).update(contains("exception_type=NULL"),eq(11L));
        verify(notifications).emitForOrder(eq(8),eq(7),eq("order"),eq("shipment:11:resolved:91"),eq("物流异常已处理"),contains("已核对取回标本"),anyString());
    }
    @Test void resolvingOneIncidentPreservesOtherUnresolvedWarning() {
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN,9));
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("id",11L,"order_id",8,"hospital_id",1,"laboratory_id",2,"courier_user_id",6,"status","IN_TRANSIT","doctor_user_id",7));
        when(jdbc.queryForList(contains("LEFT JOIN lin_audit_record r"),eq("11"))).thenReturn(List.of(Map.of("exceptionType","LOSS","note","仍需查找","reportedAt",java.sql.Timestamp.valueOf("2026-11-08 00:00:00"))));
        service.resolveException(11,71,"延误已恢复");
        verify(jdbc).update(contains("exception_type=?"),eq("LOSS"),eq("仍需查找"),any(),eq(11L));
        verify(jdbc,never()).update(contains("exception_type=NULL"),any(Object[].class));
    }
    @Test void resolutionReplayIsReadOnlyAndDifferentReasonConflicts() {
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN,9));
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("id",11L,"order_id",8,"hospital_id",1,"laboratory_id",2,"courier_user_id",6,"status","IN_TRANSIT","doctor_user_id",7));
        when(jdbc.queryForList(contains("action='EXCEPTION_RESOLVED' AND request_id=?"),eq("11"),eq("resolve_exception_71"))).thenReturn(List.of(Map.of("reason","已处理")));
        service.resolveException(11,71,"已处理");
        assertThrows(BusinessException.class,()->service.resolveException(11,71,"篡改说明"));
        verify(store,never()).insert(anyString(),any(Object[].class));
        verifyNoInteractions(notifications);
    }
    @Test void doctorCannotResolveAndWrongShipmentEventIsRejected() {
        when(store.one(contains("lin_shipment"),eq(11L))).thenReturn(Map.of("id",11L,"order_id",8,"hospital_id",1,"laboratory_id",2,"courier_user_id",6,"status","IN_TRANSIT","doctor_user_id",7));
        when(access.current()).thenReturn(actor(PlatformRole.DOCTOR,7));
        assertThrows(BusinessException.class,()->service.resolveException(11,71,"医生不能处理"));
        when(access.current()).thenReturn(actor(PlatformRole.ADMIN,9));
        when(store.one(contains("action='EXCEPTION' FOR UPDATE"),eq(99L),eq("11"))).thenThrow(new BusinessException(404,"记录不存在"));
        assertThrows(BusinessException.class,()->service.resolveException(11,99,"其他运单异常"));
        verify(store,never()).insert(anyString(),any(Object[].class));
    }
}
