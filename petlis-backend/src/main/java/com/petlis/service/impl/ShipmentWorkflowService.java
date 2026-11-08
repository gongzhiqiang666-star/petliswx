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
                + "e.laboratory_id,e.review_status,p.SampleStatus AS order_status,p.CreateSampleId AS doctor_user_id FROM lin_lb_listdiagnosisproject p "
                + "JOIN lin_order_extension e ON e.order_id=p.ListDiagnosisProjectId WHERE p.ListDiagnosisProjectId=? FOR UPDATE", orderId);
        if (!"approved".equals(order.get("review_status"))) throw new BusinessException(409, "仅审核通过的送检单可创建运单");
        if(number(order.get("order_status")).intValue()!=2) throw new BusinessException(409,"送检单需完成付款或月结挂账后才能创建运单");
        var sfRequests=store.jdbc().queryForList("SELECT state,tracking_no FROM lin_sf_request WHERE order_id=? AND operation='CREATE' ORDER BY attempt_no DESC LIMIT 1",orderId);
        if(!sfRequests.isEmpty() && (!"SUCCEEDED".equals(sfRequests.get(0).get("state")) || !command.trackingNo().trim().equals(sfRequests.get(0).get("tracking_no"))))
            throw new BusinessException(409,"该订单已有顺丰请求，请查单恢复，不得另建运单");
        if (number(order.get("laboratory_id")).intValue() != command.laboratoryId()) throw new BusinessException("运单实验室必须与送检单一致");
        long id = store.insert("INSERT INTO lin_shipment(order_id,hospital_id,laboratory_id,courier_user_id,tracking_no,status,pickup_address,expected_arrived_at,assigned_at) VALUES(?,?,?,?,?,'PENDING_PICKUP',?,?,CASE WHEN ? IS NULL THEN NULL ELSE NOW() END)",
                orderId, number(order.get("hospital_id")).intValue(), command.laboratoryId(), command.courierUserId(),
                command.trackingNo().trim(), command.pickupAddress(), timestamp(command.expectedArrivedAt()), command.courierUserId());
        timeline(id, actor.userId(), "PENDING_PICKUP", "运单已创建");
        notify(integer(order.get("doctor_user_id")), id, "PENDING_PICKUP", orderId);
        return id;
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public long linkSfRequest(int orderId,long requestId,String pickupAddress) {
        access.requireAdmin();
        var order=store.one("SELECT p.InfoHospitalId AS hospital_id,p.SampleStatus,e.review_status,e.laboratory_id,p.CreateSampleId AS doctor_user_id FROM lin_lb_listdiagnosisproject p JOIN lin_order_extension e ON e.order_id=p.ListDiagnosisProjectId WHERE p.ListDiagnosisProjectId=? FOR UPDATE",orderId);
        var request=store.one("SELECT id,attempt_no,tracking_no FROM lin_sf_request WHERE id=? AND order_id=? AND operation='CREATE' AND state='SUCCEEDED' FOR UPDATE",requestId,orderId);
        int attempt=number(request.get("attempt_no")).intValue();String tracking=String.valueOf(request.get("tracking_no"));
        if(!tracking.matches("[A-Za-z0-9-]{1,64}"))throw new BusinessException(409,"顺丰结果缺少有效主运单");
        var existing=store.jdbc().queryForList("SELECT id,tracking_no,is_current FROM lin_shipment WHERE order_id=? AND attempt_no=? FOR UPDATE",orderId,attempt);
        if(!existing.isEmpty()){
            var row=existing.get(0);if(!tracking.equals(row.get("tracking_no")) || !Integer.valueOf(1).equals(integer(row.get("is_current"))))throw new BusinessException(409,"请求与当前运单不一致，请刷新核对");
            return number(row.get("id")).longValue();
        }
        if(number(order.get("SampleStatus")).intValue()!=2 || !"approved".equals(order.get("review_status")))throw new BusinessException(409,"订单已变化，不能绑定新运单");
        if(attempt==1)return create(orderId,new ShipmentCommand(tracking,number(order.get("laboratory_id")).intValue(),pickupAddress,null,null));
        if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_lab_task WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_report_workflow WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_audit_record WHERE resource_type='MOCK_REFUND' AND action='MOCK_REFUND_COMPLETED' AND resource_id=?)",Boolean.class,orderId,orderId,String.valueOf(orderId))))throw new BusinessException(409,"订单已检测、报告或退款，请核对顺丰结果");
        var audit=store.one("SELECT JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.previousCreateId')) AS previousCreateId,reason FROM lin_audit_record WHERE resource_type='SF_REQUEST' AND resource_id=? AND action='SF_RESHIP_RESERVED'",String.valueOf(requestId));
        long previous=Long.parseLong(String.valueOf(audit.get("previousCreateId")));
        var old=store.one("SELECT s.id FROM lin_shipment s JOIN lin_sf_request r ON r.order_id=s.order_id AND r.attempt_no=s.attempt_no AND r.tracking_no=s.tracking_no AND r.operation='CREATE' AND r.state='SUCCEEDED' JOIN lin_sf_request c ON c.order_id=r.order_id AND c.attempt_no=r.attempt_no AND c.external_order_no=r.external_order_no AND c.operation='CANCEL' AND c.state='SUCCEEDED' WHERE r.id=? AND s.order_id=? AND s.attempt_no=? AND s.is_current=1 AND s.status='CANCELLED' FOR UPDATE",previous,orderId,attempt-1);
        store.jdbc().update("UPDATE lin_shipment SET is_current=NULL,updated_at=updated_at WHERE id=?",old.get("id"));
        long id=store.insert("INSERT INTO lin_shipment(order_id,hospital_id,laboratory_id,tracking_no,status,pickup_address,attempt_no,is_current) VALUES(?,?,?,?,'PENDING_PICKUP',?,?,1)",orderId,order.get("hospital_id"),order.get("laboratory_id"),tracking,pickupAddress,attempt);
        timeline(id,access.current().userId(),"PENDING_PICKUP","第"+attempt+"次寄件："+audit.get("reason"));notify(integer(order.get("doctor_user_id")),id,"PENDING_PICKUP",orderId);return id;
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
        return transition(shipmentId,action,exceptionType,note,null);
    }
    @Transactional
    public Map<String, Object> transition(long shipmentId, ShipmentAction action, String exceptionType, String note,String requestKey) {
        CurrentActor actor = access.current();
        Map<String, Object> shipment = locked(shipmentId);
        if (action == ShipmentAction.RECEIVE) {
            require(actor, PlatformRole.LAB_STAFF);
            access.requireLaboratory(number(shipment.get("laboratory_id")).intValue());
        } else {
            require(actor, PlatformRole.COURIER);
            access.requireCourierAssignment(integer(shipment.get("courier_user_id")));
        }
        if(action!=ShipmentAction.RECEIVE) {
            var cancellation=store.jdbc().queryForList("SELECT state FROM lin_sf_request WHERE order_id=? AND attempt_no=? AND operation='CANCEL' AND state<>'REJECTED'",shipment.get("order_id"),shipment.get("attempt_no"));
            if(!cancellation.isEmpty()) throw new BusinessException(409,"顺丰取消处理中或已取消，不能继续运输操作");
        }
        if (action == ShipmentAction.RECEIVE && "RECEIVED".equals(shipment.get("status"))) return get(shipmentId);
        if (action == ShipmentAction.EXCEPTION) {
            if (blank(note) || blank(exceptionType) || note.length()>1000 || exceptionType.trim().length()>32) throw new BusinessException(400,"异常类型和说明不能为空且不能过长");
            String type=exceptionType.trim(), explanation=note.trim();
            if(requestKey!=null && !requestKey.matches("[A-Za-z0-9_-]{16,64}")) throw new BusinessException(400,"异常请求号无效");
            String key=requestKey==null?legacyExceptionKey(type,explanation):requestKey;
            var previous=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id=? AND action='EXCEPTION' AND request_id=? FOR UPDATE",String.valueOf(shipmentId),key);
            if(!previous.isEmpty()) {
                try {
                    var value=new com.fasterxml.jackson.databind.ObjectMapper().readTree(String.valueOf(previous.get(0).get("after_json")));
                    if(!type.equals(value.path("exceptionType").asText()) || !explanation.equals(value.path("note").asText())) throw new BusinessException(409,"该请求号已用于不同的异常，请重新登记");
                } catch(com.fasterxml.jackson.core.JsonProcessingException error) { throw new BusinessException(409,"原异常记录无法校验，请核对进度"); }
                return get(shipmentId);
            }
            String status = String.valueOf(shipment.get("status"));
            if (!List.of("PENDING_PICKUP", "PICKED_UP", "IN_TRANSIT", "DELIVERED").contains(status)) throw invalidState();
            store.jdbc().update("UPDATE lin_shipment SET exception_type=?,exception_note=?,exception_at=NOW(),version=version+1 WHERE id=?",type,explanation,shipmentId);
            long eventId=store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json,reason) VALUES(?,'EXCEPTION','SHIPMENT',?,?,?,?)",actor.userId(),String.valueOf(shipmentId),key,store.encode(Map.of("exceptionType",type,"note",explanation)),explanation);
            notifications.emitForOrder(number(shipment.get("order_id")).intValue(),integer(shipment.get("doctor_user_id")),"order","shipment:"+shipmentId+":exception:"+eventId,"标本物流异常",exceptionMessage(type,explanation),"pages/order-detail/order-detail?id="+shipment.get("order_id"));
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
            // Share the actual laboratory signature time with PHP/monthly reports. Carrier delivery
            // is not laboratory receipt; a valid date already recorded by PHP remains authoritative.
            store.jdbc().update("UPDATE lin_lb_listdiagnosisproject p JOIN lin_shipment s ON s.order_id=p.ListDiagnosisProjectId SET p.ReceiveSampleDt=s.signed_at WHERE s.id=? AND s.status='RECEIVED' AND s.signed_at IS NOT NULL AND (p.ReceiveSampleDt IS NULL OR p.ReceiveSampleDt<'1901-01-01 00:00:00')", shipmentId);
            store.insert("INSERT INTO lin_lab_task(shipment_id,order_id,laboratory_id,task_type,status,received_by,received_at) VALUES(?,?,?,'INITIAL','RECEIVED',?,NOW())",
                    shipmentId, number(shipment.get("order_id")).intValue(), number(shipment.get("laboratory_id")).intValue(), actor.userId());
        }
        notify(integer(shipment.get("doctor_user_id")), shipmentId, target, number(shipment.get("order_id")).intValue());
        return get(shipmentId);
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Map<String,Object> resolveException(long shipmentId,long eventId,String reason) {
        CurrentActor actor=access.current();
        var shipment=locked(shipmentId);
        if(!actor.isAdmin()) {
            if(actor.hasRole(PlatformRole.LAB_STAFF)) access.requireLaboratory(number(shipment.get("laboratory_id")).intValue());
            else {require(actor,PlatformRole.COURIER);access.requireCourierAssignment(integer(shipment.get("courier_user_id")));}
        }
        if(eventId<1 || blank(reason) || reason.trim().length()>1000) throw new BusinessException(400,"请填写1000字以内的异常处理说明");
        reason=reason.trim();
        store.one("SELECT id FROM lin_audit_record WHERE id=? AND resource_type='SHIPMENT' AND resource_id=? AND action='EXCEPTION' FOR UPDATE",eventId,String.valueOf(shipmentId));
        String key="resolve_exception_"+eventId;
        var existing=store.jdbc().queryForList("SELECT reason FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id=? AND action='EXCEPTION_RESOLVED' AND request_id=? FOR UPDATE",String.valueOf(shipmentId),key);
        if(!existing.isEmpty()) {
            if(!reason.equals(existing.get(0).get("reason"))) throw new BusinessException(409,"该异常已使用其他说明处理，请查看历史记录");
            return get(shipmentId);
        }
        long resolutionId=store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json,reason) VALUES(?,'EXCEPTION_RESOLVED','SHIPMENT',?,?,?,?)",actor.userId(),String.valueOf(shipmentId),key,store.encode(Map.of("exceptionEventId",eventId)),reason);
        var unresolved=exceptionEvents(shipmentId).stream().filter(row->row.get("resolvedAt")==null).findFirst();
        if(unresolved.isPresent()) {
            var event=unresolved.get();
            store.jdbc().update("UPDATE lin_shipment SET exception_type=?,exception_note=?,exception_at=?,version=version+1 WHERE id=?",event.get("exceptionType"),event.get("note"),event.get("reportedAt"),shipmentId);
        } else store.jdbc().update("UPDATE lin_shipment SET exception_type=NULL,exception_note=NULL,exception_at=NULL,version=version+1 WHERE id=?",shipmentId);
        notifications.emitForOrder(number(shipment.get("order_id")).intValue(),integer(shipment.get("doctor_user_id")),"order","shipment:"+shipmentId+":resolved:"+resolutionId,"物流异常已处理",exceptionMessage("OTHER",reason),"pages/order-detail/order-detail?id="+shipment.get("order_id"));
        return get(shipmentId);
    }
    private List<Map<String,Object>> exceptionEvents(long shipmentId) {
        return store.jdbc().queryForList("SELECT e.id,COALESCE(JSON_UNQUOTE(JSON_EXTRACT(e.after_json,'$.exceptionType')),'OTHER') AS exceptionType,e.reason AS note,e.actor_user_id AS reportedBy,e.created_at AS reportedAt,r.reason AS resolutionNote,r.actor_user_id AS resolvedBy,r.created_at AS resolvedAt FROM lin_audit_record e LEFT JOIN lin_audit_record r ON r.resource_type='SHIPMENT' AND r.resource_id=e.resource_id AND r.action='EXCEPTION_RESOLVED' AND r.request_id=CONCAT('resolve_exception_',e.id) WHERE e.resource_type='SHIPMENT' AND e.resource_id=? AND e.action='EXCEPTION' ORDER BY e.id DESC",String.valueOf(shipmentId)).stream().map(row->{
            Map<String,Object> result=new java.util.LinkedHashMap<>(row);
            result.put("resolved",row.get("resolvedAt")!=null);
            return result;
        }).toList();
    }

    @Transactional
    public Map<String,Object> applySfCancellation(long shipmentId) {
        CurrentActor actor=access.current();require(actor,PlatformRole.ADMIN);
        var shipment=locked(shipmentId);
        var confirmed=store.jdbc().queryForList("SELECT id FROM lin_sf_request WHERE order_id=? AND attempt_no=? AND operation='CANCEL' AND state='SUCCEEDED'",shipment.get("order_id"),shipment.get("attempt_no"));
        if(confirmed.isEmpty()) throw new BusinessException(409,"顺丰尚未确认取消，不能变更运单状态");
        if("CANCELLED".equals(shipment.get("status"))) return get(shipmentId);
        if(!"PENDING_PICKUP".equals(shipment.get("status"))) throw invalidState();
        store.jdbc().update("UPDATE lin_shipment SET status='CANCELLED',version=version+1 WHERE id=?",shipmentId);
        timeline(shipmentId,actor.userId(),"CANCELLED","顺丰已确认取消寄件，送检单仍保留");
        notify(integer(shipment.get("doctor_user_id")),shipmentId,"CANCELLED",number(shipment.get("order_id")).intValue());
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
        Map<String, Object> row = store.one("SELECT s.*,o.CreateSampleId AS doctor_user_id FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.order_id=? AND s.is_current=1", orderId);
        authorizeRead(row);
        return view(row);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> history(int orderId) {
        var order=store.one("SELECT InfoHospitalId AS hospital_id,CreateSampleId AS doctor_user_id FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",orderId);
        CurrentActor actor=access.current();
        var rows=store.jdbc().queryForList("SELECT s.*,o.CreateSampleId AS doctor_user_id FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.order_id=? ORDER BY s.attempt_no DESC",orderId);
        if(rows.isEmpty()) {access.requireHospital(number(order.get("hospital_id")).intValue());return List.of();}
        for(var row:rows)authorizeRead(row);
        return rows.stream().map(this::view).toList();
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
        StringBuilder where = new StringBuilder(" FROM lin_shipment s WHERE s.is_current=1");
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
        String columns = "SELECT s.id,s.attempt_no AS attemptNo,s.is_current AS isCurrent,s.order_id AS orderId,s.hospital_id AS hospitalId,s.laboratory_id AS laboratoryId,s.courier_user_id AS courierUserId,s.tracking_no AS trackingNo,s.status,s.expected_arrived_at AS expectedArrivedAt,s.exception_type AS exceptionType,s.exception_note AS exceptionNote,s.created_at AS createdAt,EXISTS(SELECT 1 FROM lin_sf_request r WHERE r.order_id=s.order_id AND r.tracking_no=s.tracking_no AND r.operation='CREATE' AND r.state='SUCCEEDED') AS sandbox";
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
        var row=store.one("SELECT s.*,o.CreateSampleId AS doctor_user_id FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.id=? FOR UPDATE", id);
        if(row.containsKey("is_current") && !Integer.valueOf(1).equals(integer(row.get("is_current"))))throw new BusinessException(409,"历史运单仅可查看，不能继续操作");
        return row;
    }

    private Map<String, Object> view(Map<String, Object> row) {
        var result = new java.util.LinkedHashMap<String, Object>();
        row.forEach((key, value) -> result.put(toCamel(key), value));
        result.put("current",Integer.valueOf(1).equals(integer(row.getOrDefault("is_current",1))));
        result.put("sandbox", Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_sf_request WHERE order_id=? AND tracking_no=? AND operation='CREATE' AND state='SUCCEEDED')",Boolean.class,row.get("order_id"),row.get("tracking_no"))));
        result.put("exceptions",exceptionEvents(((Number)row.get("id")).longValue()));
        result.put("timeline", store.jdbc().queryForList("SELECT action,reason,createdAt FROM (SELECT action,reason,created_at AS createdAt FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id=? UNION ALL SELECT COALESCE(mapped_status,'SF_ROUTE') AS action,description AS reason,event_time AS createdAt FROM lin_sf_route_event WHERE shipment_id=? AND event_time>=(SELECT created_at FROM lin_shipment WHERE id=?)) events ORDER BY createdAt", String.valueOf(row.get("id")),row.get("id"),row.get("id")));

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

    private static String legacyExceptionKey(String type,String note) {
        try {
            String text=type.length()+":"+type+note.length()+":"+note;
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    static String exceptionMessage(String type,String note) {
        String text=exceptionLabel(type)+"："+note;
        if(text.length()<=1000) return text;
        int end=999;
        if(Character.isHighSurrogate(text.charAt(end-1))) end--;
        return text.substring(0,end)+"…";
    }
    static String exceptionLabel(String type) {
        return switch(type) {case "DAMAGE"->"标本破损";case "LOSS"->"标本丢失";case "DELAY"->"超时未送达";case "OTHER"->"其他异常";default->type;};
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
