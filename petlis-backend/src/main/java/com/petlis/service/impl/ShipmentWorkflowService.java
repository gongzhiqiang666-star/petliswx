package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.common.security.CurrentActor;
import com.petlis.common.security.PlatformRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShipmentWorkflowService {
    private final FeatureStore store;
    private final AccessService access;
    private final BusinessNotifications notifications;

    public record ShipmentCommand(String trackingNo, Integer laboratoryId, String pickupAddress,
                                  String expectedArrivedAt, Integer courierUserId) { }
    public enum ShipmentAction { PICKUP, TRANSIT, DELIVER, RECEIVE, EXCEPTION }

    @Transactional
    public long create(int orderId, ShipmentCommand command) {
        CurrentActor actor = access.current();
        require(actor, PlatformRole.ADMIN);
        if (command == null || blank(command.trackingNo()) || command.laboratoryId() == null
                || command.trackingNo().length() > 64) throw new BusinessException("运单号、实验室和揽收地址不能为空");
        Map<String, Object> order = store.one("SELECT p.ListDiagnosisProjectId AS order_id,p.InfoHospitalId AS hospital_id,"
                + "e.laboratory_id,e.review_status,p.CreateSampleId AS doctor_user_id FROM lin_lb_listdiagnosisproject p "
                + "JOIN lin_order_extension e ON e.order_id=p.ListDiagnosisProjectId WHERE p.ListDiagnosisProjectId=? FOR UPDATE", orderId);
        if (!"approved".equals(order.get("review_status"))) throw new BusinessException(409, "仅审核通过的送检单可创建运单");
        if (number(order.get("laboratory_id")).intValue() != command.laboratoryId()) throw new BusinessException("运单实验室必须与送检单一致");
        long id = store.insert("INSERT INTO lin_shipment(order_id,hospital_id,laboratory_id,courier_user_id,tracking_no,status,pickup_address,expected_arrived_at,assigned_at) VALUES(?,?,?,?,?,'PENDING_PICKUP',?,?,CASE WHEN ? IS NULL THEN NULL ELSE NOW() END)",
                orderId, number(order.get("hospital_id")).intValue(), command.laboratoryId(), command.courierUserId(),
                command.trackingNo().trim(), command.pickupAddress(), timestamp(command.expectedArrivedAt()), command.courierUserId());
        timeline(id, actor.userId(), "PENDING_PICKUP", "运单已创建");
        notify(integer(order.get("doctor_user_id")), id, "PENDING_PICKUP", orderId);
        return id;
    }

    @Transactional
    public void assign(long shipmentId, int courierUserId) {
        CurrentActor actor = access.current();
        require(actor, PlatformRole.ADMIN);
        Map<String, Object> shipment = locked(shipmentId);
        if (!"PENDING_PICKUP".equals(shipment.get("status"))) throw invalidState();
        if (!courierExists(courierUserId)) throw new BusinessException("快递员不存在或角色未启用");
        store.jdbc().update("UPDATE lin_shipment SET courier_user_id=?,assigned_at=NOW(),version=version+1 WHERE id=?", courierUserId, shipmentId);
        timeline(shipmentId, actor.userId(), "ASSIGNED", "运单已分配");
        notify(integer(shipment.get("doctor_user_id")), shipmentId, "ASSIGNED", number(shipment.get("order_id")).intValue());
    }

    @Transactional
    public Map<String, Object> transition(long shipmentId, ShipmentAction action, String exceptionType, String note) {
        CurrentActor actor = access.current();
        Map<String, Object> shipment = locked(shipmentId);
        if (action == ShipmentAction.RECEIVE) {
            require(actor, PlatformRole.LAB_STAFF);
            access.requireLaboratory(number(shipment.get("laboratory_id")).intValue());
        } else {
            require(actor, PlatformRole.COURIER);
            access.requireCourierAssignment(integer(shipment.get("courier_user_id")));
        }
        if (action == ShipmentAction.RECEIVE && "RECEIVED".equals(shipment.get("status"))) return get(shipmentId);
        if (action == ShipmentAction.EXCEPTION) {
            if (blank(note) || blank(exceptionType) || note.length() > 1000) throw new BusinessException("异常类型和说明不能为空");
            String status = String.valueOf(shipment.get("status"));
            if (!List.of("PENDING_PICKUP", "PICKED_UP", "IN_TRANSIT", "DELIVERED").contains(status)) throw invalidState();
            store.jdbc().update("UPDATE lin_shipment SET exception_type=?,exception_note=?,exception_at=NOW(),version=version+1 WHERE id=?",
                    exceptionType.trim(), note.trim(), shipmentId);
            timeline(shipmentId, actor.userId(), "EXCEPTION", note.trim());
            notify(integer(shipment.get("doctor_user_id")), shipmentId, "EXCEPTION", number(shipment.get("order_id")).intValue());
            return get(shipmentId);
        }
        String target = nextStatus(String.valueOf(shipment.get("status")), action);
        String column = switch (action) {
            case PICKUP -> "picked_up_at=NOW()";
            case DELIVER -> "delivered_at=NOW()";
            case RECEIVE -> "signed_by=" + actor.userId() + ",signed_at=NOW()";
            default -> "";
        };
        store.jdbc().update("UPDATE lin_shipment SET status=?," + (column.isEmpty() ? "version=version+1" : column + ",version=version+1") + " WHERE id=?", target, shipmentId);
        timeline(shipmentId, actor.userId(), target, action.name());
        if (action == ShipmentAction.RECEIVE) {
            store.insert("INSERT INTO lin_lab_task(shipment_id,order_id,laboratory_id,task_type,status,received_by,received_at) VALUES(?,?,?,'INITIAL','RECEIVED',?,NOW())",
                    shipmentId, number(shipment.get("order_id")).intValue(), number(shipment.get("laboratory_id")).intValue(), actor.userId());
        }
        notify(integer(shipment.get("doctor_user_id")), shipmentId, target, number(shipment.get("order_id")).intValue());
        return get(shipmentId);
    }

    @Transactional
    public Map<String, Object> receiveScanned(long shipmentId, String barcode) {
        if (blank(barcode)) throw new BusinessException(400, "扫码条码不能为空");
        Map<String, Object> shipment = store.one("SELECT s.id,o.UserNO AS order_user_no FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.id=? FOR UPDATE", shipmentId);
        if (shipment.get("order_user_no") == null || !barcode.trim().equals(shipment.get("order_user_no").toString())) throw new BusinessException(400, "扫码条码与送检单不匹配");
        return transition(shipmentId, ShipmentAction.RECEIVE, null, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getByOrder(int orderId) {
        Map<String, Object> row = store.one("SELECT s.*,o.CreateSampleId AS doctor_user_id FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.order_id=?", orderId);
        authorizeRead(row);
        return view(row);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(long shipmentId) {
        Map<String, Object> row = store.one("SELECT s.*,o.CreateSampleId AS doctor_user_id FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.id=?", shipmentId);
        authorizeRead(row);
        return view(row);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String status, int page, int size) {
        CurrentActor actor = access.current();
        StringBuilder where = new StringBuilder(" FROM lin_shipment s WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!blank(status)) { where.append(" AND s.status=?"); args.add(status); }
        if (actor.isAdmin()) {
        } else if (actor.hasRole(PlatformRole.COURIER)) {
            where.append(" AND s.courier_user_id=?"); args.add(actor.userId());
        } else if (actor.hasRole(PlatformRole.LAB_STAFF)) {
            where.append(" AND s.laboratory_id IN (SELECT laboratory_id FROM lin_user_role WHERE user_id=? AND role_code='LAB_STAFF' AND is_active=1)"); args.add(actor.userId());
        } else if (actor.hasRole(PlatformRole.DOCTOR) || actor.hasRole(PlatformRole.HOSPITAL_ADMIN)) {
            where.append(" AND s.hospital_id IN (SELECT hospital_id FROM lin_user_role WHERE user_id=? AND role_code IN ('DOCTOR','HOSPITAL_ADMIN') AND is_active=1)"); args.add(actor.userId());
        } else throw new BusinessException(403, "无权查看物流");
        String columns = "SELECT s.id,s.order_id AS orderId,s.hospital_id AS hospitalId,s.laboratory_id AS laboratoryId,s.courier_user_id AS courierUserId,s.tracking_no AS trackingNo,s.status,s.expected_arrived_at AS expectedArrivedAt,s.exception_type AS exceptionType,s.exception_note AS exceptionNote,s.created_at AS createdAt";
        return store.page("SELECT COUNT(*)" + where, columns + where + " ORDER BY s.id DESC", page, size, args.toArray());
    }

    String nextStatus(String from, ShipmentAction action) {
        String target = switch (action) {
            case PICKUP -> "PENDING_PICKUP".equals(from) ? "PICKED_UP" : null;
            case TRANSIT -> "PICKED_UP".equals(from) ? "IN_TRANSIT" : null;
            case DELIVER -> "IN_TRANSIT".equals(from) ? "DELIVERED" : null;
            case RECEIVE -> "DELIVERED".equals(from) ? "RECEIVED" : null;
            default -> null;
        };
        if (target == null) throw invalidState();
        return target;
    }

    private Map<String, Object> locked(long id) {
        return store.one("SELECT s.*,o.CreateSampleId AS doctor_user_id FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.id=? FOR UPDATE", id);
    }

    private Map<String, Object> view(Map<String, Object> row) {
        var result = new java.util.LinkedHashMap<String, Object>();
        row.forEach((key, value) -> result.put(toCamel(key), value));
        result.put("timeline", store.jdbc().queryForList("SELECT action,reason,created_at AS createdAt FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id=? ORDER BY id", String.valueOf(row.get("id"))));
        return result;
    }

    private void authorizeRead(Map<String, Object> row) {
        CurrentActor actor = access.current();
        if (actor.isAdmin()) return;
        if (actor.hasRole(PlatformRole.COURIER)) access.requireCourierAssignment(integer(row.get("courier_user_id")));
        else if (actor.hasRole(PlatformRole.LAB_STAFF)) access.requireLaboratory(number(row.get("laboratory_id")).intValue());
        else access.requireHospital(number(row.get("hospital_id")).intValue());
    }

    private boolean courierExists(int userId) {
        return Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_user_role WHERE user_id=? AND role_code='COURIER' AND is_active=1)", Boolean.class, userId));
    }

    private void timeline(long shipmentId, int actor, String action, String reason) {
        store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,reason) VALUES(?,?,'SHIPMENT',?,?)",
                actor, action, String.valueOf(shipmentId), reason);
    }

    private void notify(Integer userId, long shipmentId, String action, int orderId) {
        notifications.emitForOrder(orderId, userId, "order", "shipment:" + shipmentId + ":" + action, "物流状态已更新",
                "送检单 #" + orderId + " 物流节点：" + action, "pages/order-detail/order-detail?id=" + orderId);
    }

    private LocalDateTime timestamp(String value) {
        if (blank(value)) return null;
        try { return LocalDateTime.parse(value); } catch (Exception error) { throw new BusinessException("预计到达时间格式无效"); }
    }
    private void require(CurrentActor actor, PlatformRole role) { if (!actor.hasRole(role)) throw new BusinessException(403, "角色无权执行该物流操作"); }
    private BusinessException invalidState() { return new BusinessException(409, "物流状态不允许该操作"); }
    private Integer integer(Object value) { return value == null ? null : number(value).intValue(); }
    private Number number(Object value) { return (Number) value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String toCamel(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.toLowerCase().split("_")) {
            if (result.isEmpty()) result.append(part); else if (!part.isEmpty()) result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
