package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;

/** Accepts only routes fetched over the authenticated SF gateway; never bind directly to unverified HTTP. */
@Service
@RequiredArgsConstructor
public class SfRouteEventService {
    private final FeatureStore store;
    private final BusinessNotifications notifications;
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private static final Map<String,Integer> RANK=Map.of("PENDING_PICKUP",0,"PICKED_UP",1,"IN_TRANSIT",2,"DELIVERED",3,"RECEIVED",4);

    /** Called only after the authenticated route-query gateway succeeds; no carrier HTTP in this transaction. */
    @Transactional
    public void applyVerifiedBatch(long shipmentId,String trackingNo,JsonNode data) {
        if(data==null || !data.path("routeResps").isArray()) throw new BusinessException(409,"顺丰路由响应格式无效");
        var batch=new ArrayList<JsonNode>();
        for(var response:data.path("routeResps")) {
            if(!response.path("mailNo").isTextual() || !Objects.equals(trackingNo,response.path("mailNo").textValue()))
                throw new BusinessException(409,"路由响应运单不匹配，未应用任何节点");
            if(!response.path("routes").isArray()) throw new BusinessException(409,"顺丰路由节点格式无效");
            for(var route:response.path("routes")) {
                validateRoute(route);
                batch.add(route);
            }
        }
        var linked=store.one("SELECT s.tracking_no,r.external_order_no FROM lin_shipment s JOIN lin_sf_request r ON r.order_id=s.order_id AND r.attempt_no=s.attempt_no AND r.tracking_no=s.tracking_no AND r.operation='CREATE' AND r.state='SUCCEEDED' WHERE s.id=? FOR UPDATE",shipmentId);
        if(!Objects.equals(trackingNo,linked.get("tracking_no"))) throw new BusinessException(409,"运单关联已变化，请重新查询");
        for(var route:batch) applyVerified(shipmentId,route);
    }
    private static void validateRoute(JsonNode route) {
        if(!route.isObject()) throw new BusinessException(400,"顺丰路由节点无效");
        String code=route.path("opCode").asText(route.path("opcode").asText());
        if(code.isBlank() || code.length()>32) throw new BusinessException(400,"顺丰路由码无效");
        try { LocalDateTime.parse(route.path("acceptTime").asText(),TIME); }
        catch(Exception error) { throw new BusinessException(400,"顺丰路由时间无效"); }
    }
    private void applyVerified(long shipmentId,JsonNode route) {
        String code=route.path("opCode").asText(route.path("opcode").asText());
        if(code.isBlank() || code.length()>32) throw new BusinessException(400,"顺丰路由码无效");
        LocalDateTime occurred;
        try {occurred=LocalDateTime.parse(route.path("acceptTime").asText(),TIME);}
        catch(Exception e) {throw new BusinessException(400,"顺丰路由时间无效");}
        var shipment=store.one("SELECT s.*,o.CreateSampleId AS doctor_user_id FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.id=? FOR UPDATE",shipmentId);
        String description=route.path("remark").asText("");
        if(description.length()>1000) description=description.substring(0,1000);
        String key=eventKey(String.valueOf(shipment.get("tracking_no")),code,occurred,description,route.path("acceptAddress").asText(""));
        String mapped=mappedStatus(route);
        // Locking the shipment serializes route inserts from manual polling and future verified callbacks.
        var exists=store.jdbc().queryForList("SELECT id FROM lin_sf_route_event WHERE event_key=? FOR UPDATE",key);
        if(!exists.isEmpty()) return;
        long id=store.insert("INSERT INTO lin_sf_route_event(shipment_id,event_key,carrier_code,event_time,mapped_status,description) VALUES(?,?,?,?,?,?)",shipmentId,key,code,occurred,mapped,description);
        Timestamp latest=store.jdbc().queryForObject("SELECT MAX(event_time) FROM lin_sf_route_event WHERE shipment_id=? AND applied_at IS NOT NULL",Timestamp.class,shipmentId);
        String current=String.valueOf(shipment.get("status"));
        Object created=shipment.get("created_at");
        LocalDateTime createdAt=created instanceof Timestamp t?t.toLocalDateTime():(created instanceof LocalDateTime t?t:null);
        if(createdAt!=null && occurred.isBefore(createdAt)) return; // Sandbox may return canned historical events.
        if(occurred.isAfter(LocalDateTime.now().plusMinutes(10))) return;
        if(shipment.containsKey("is_current") && !Integer.valueOf(1).equals(shipment.get("is_current")))return;
        if(!store.jdbc().queryForList("SELECT id FROM lin_sf_request WHERE order_id=? AND attempt_no=? AND operation='CANCEL' AND state<>'REJECTED'",shipment.get("order_id"),shipment.get("attempt_no")).isEmpty()) return;
        if(!canAdvance(current,mapped,occurred,latest==null?null:latest.toLocalDateTime())) return;
        String timestampColumn="DELIVERED".equals(mapped)?",delivered_at=?":("PICKED_UP".equals(mapped)?",picked_up_at=?":"");
        if(timestampColumn.isEmpty()) store.jdbc().update("UPDATE lin_shipment SET status=?,version=version+1 WHERE id=?",mapped,shipmentId);
        else store.jdbc().update("UPDATE lin_shipment SET status=?"+timestampColumn+",version=version+1 WHERE id=?",mapped,occurred,shipmentId);
        store.jdbc().update("UPDATE lin_sf_route_event SET applied_at=NOW(6) WHERE id=?",id);
        Number doctor=(Number)shipment.get("doctor_user_id");
        notifications.emitForOrder(((Number)shipment.get("order_id")).intValue(),doctor==null?null:doctor.intValue(),"order","sf-route:"+key,"物流状态已更新",description,"pages/order-detail/order-detail?id="+shipment.get("order_id"));
    }
    static boolean canAdvance(String current,String target,LocalDateTime occurred,LocalDateTime latest) {
        return target!=null && RANK.containsKey(current) && RANK.get(target)>RANK.get(current)
                && (latest==null || !occurred.isBefore(latest));
    }
    static String mappedStatus(JsonNode route) {
        // Current sandbox response includes primary status codes. Unknown categories remain timeline-only.
        String primary=route.path("firstStatusCode").asText();
        return switch(primary) {
            case "1" -> "PICKED_UP";
            case "2","3" -> "IN_TRANSIT";
            case "4" -> "DELIVERED";
            default -> null;
        };
    }
    static String eventKey(String tracking,String code,LocalDateTime time,String description,String address) {
        try {
            // Length prefixes prevent ambiguous concatenations while ignoring JSON key order and receipt time.
            var raw=new StringBuilder();
            for(String value:List.of(tracking,code,time.toString(),description,address)) raw.append(value.length()).append(':').append(value);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.toString().getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException impossible) {throw new IllegalStateException(impossible);}
    }
}
