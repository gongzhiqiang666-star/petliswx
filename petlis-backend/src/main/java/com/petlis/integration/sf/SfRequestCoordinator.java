package com.petlis.integration.sf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Durable single-dispatch boundary. Call only after business authorization and validation. */
public final class SfRequestCoordinator {
    public record Attempt(int number,Long previousCreateId,String reason) {
        public static Attempt first(){return new Attempt(1,null,"");}
    }
    public record Request(long id, String operation, String key, String externalOrderNo, String payloadHash, String state,
                          String trackingNo, String resultJson) {}
    public interface Store {
        Request reserve(int orderId, String operation, String key, String externalOrderNo, String hash, int actorId);
        default Request reserveAttempt(int orderId,String operation,String key,String external,String hash,int actorId,Attempt attempt){
            if(attempt.number()!=1)throw new IllegalStateException("请求存储尚不支持多次寄件");
            return reserve(orderId,operation,key,external,hash,actorId);
        }
        Request get(long id);
        boolean claim(long id);
        void finish(long id, String state, String platformCode, String businessCode, String trackingNo, String resultJson);
        boolean reconcile(long id, String trackingNo, String resultJson);
        boolean reconcileCancellation(long id, String resultJson);
    }
    private final Store store;
    private final SfSandboxGateway gateway;
    private final ObjectMapper mapper;
    public SfRequestCoordinator(Store store, SfSandboxGateway gateway, ObjectMapper mapper) {
        this.store=store; this.gateway=gateway;
        this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }
    public Request dispatch(int orderId, int actorId, String operation, Map<String,Object> validatedPayload) throws Exception {
        return dispatch(orderId,actorId,operation,validatedPayload,Attempt.first());
    }
    public Request dispatch(int orderId,int actorId,String operation,Map<String,Object> validatedPayload,Attempt attempt)throws Exception {
        if(orderId<=0 || actorId<=0) throw new IllegalArgumentException("订单或操作人无效");
        if(attempt==null || attempt.number()<1 || attempt.number()>10)throw new IllegalArgumentException("寄件次数无效");
        if(attempt.number()>1 && "CREATE".equals(operation) && (attempt.previousCreateId()==null || attempt.previousCreateId()<=0 || attempt.reason()==null || attempt.reason().isBlank() || attempt.reason().length()>500))throw new IllegalArgumentException("再次寄件需提供前次请求和原因");
        String service=switch(operation) {
            case "CREATE" -> "EXP_RECE_CREATE_ORDER";
            case "CANCEL" -> "EXP_RECE_UPDATE_ORDER";
            default -> throw new IllegalArgumentException("不支持的顺丰操作");
        };
        String external="PETLIS_SANDBOX_"+orderId+(attempt.number()==1?"":"_A"+attempt.number());
        var payload=new TreeMap<String,Object>(validatedPayload);
        payload.put("orderId",external);
        if(operation.equals("CANCEL")) payload.put("dealType",2);
        Object fingerprint=attempt.number()>1 && "CREATE".equals(operation)?Map.of("payload",payload,"previousCreateId",attempt.previousCreateId(),"reason",attempt.reason().trim()):payload;
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsString(fingerprint).getBytes(StandardCharsets.UTF_8)));
        Request request=store.reserveAttempt(orderId,operation,UUID.randomUUID().toString(),external,hash,actorId,attempt);
        if(!request.payloadHash().equals(hash)) throw new IllegalStateException("该订单已有不同寄件参数，请先核实原请求");
        // Only RESERVED may transition to SENDING; concurrent callers and crash recovery never resend.
        if(!store.claim(request.id())) return store.get(request.id());
        SfSandboxGateway.Outcome outcome;
        String tracking=null, result=null;
        try {
            outcome=gateway.call(service,payload,request.key(),System.currentTimeMillis());
            if(outcome.success()) {
                if(!external.equals(outcome.data().path("orderId").asText())) throw new IllegalStateException("响应订单不匹配");
                if(operation.equals("CREATE")) tracking=trackingNo(outcome.data());
                result=safeResult(external,tracking,operation.equals("CANCEL"));
            }
        } catch(Exception error) {
            store.finish(request.id(),"UNKNOWN",null,null,null,null);
            // Do not propagate carrier payloads or credentials in exception messages.
            throw new IllegalStateException("顺丰请求结果未知，请先查单，禁止重复寄件");
        }
        String state=outcome.success()?"SUCCEEDED":"REJECTED";
        store.finish(request.id(),state,outcome.platformCode(),outcome.businessCode(),tracking,result);
        return store.get(request.id());
    }

    /** Read-only carrier lookup; absence/rejection never grants permission to create again. */
    public Request reconcileCreate(long requestId) throws Exception {
        Request request=store.get(requestId);
        if(!"CREATE".equals(request.operation())) throw new IllegalArgumentException("仅支持恢复寄件请求");
        if(!Set.of("SENDING","UNKNOWN").contains(request.state())) return request;
        var outcome=gateway.call("EXP_RECE_SEARCH_ORDER_RESP",Map.of("orderId",request.externalOrderNo(),"searchType","1","language","zh-CN"),UUID.randomUUID().toString(),System.currentTimeMillis());
        if(!outcome.success()) return store.get(requestId);
        if(!request.externalOrderNo().equals(outcome.data().path("orderId").asText())) throw new IllegalStateException("顺丰查单响应不匹配");
        String tracking=trackingNo(outcome.data());
        store.reconcile(requestId,tracking,safeResult(request.externalOrderNo(),tracking,false));
        return store.get(requestId);
    }

    /** A lookup for this durable external order must explicitly return SF's cancelled code. */
    public Request reconcileCancellation(long requestId) throws Exception {
        Request request=store.get(requestId);
        if(!"CANCEL".equals(request.operation())) throw new IllegalArgumentException("仅支持核对取消请求");
        if(!Set.of("SENDING","UNKNOWN").contains(request.state())) return request;
        var outcome=gateway.call("EXP_RECE_SEARCH_ORDER_RESP",Map.of("orderId",request.externalOrderNo(),"searchType","1","language","zh-CN"),UUID.randomUUID().toString(),System.currentTimeMillis());
        // A returned waybill, not-found, timeout, or platform failure never proves cancellation.
        if("A1000".equals(outcome.platformCode()) && "8037".equals(outcome.businessCode()) && !outcome.success())
            store.reconcileCancellation(requestId,safeResult(request.externalOrderNo(),null,true));
        return store.get(requestId);
    }
    private String trackingNo(JsonNode data) {
        // Only the parent waybill is linked to the specimen shipment.
        for(JsonNode waybill:data.path("waybillNoInfoList")) {
            String value=waybill.path("waybillNo").asText();
            if(waybill.path("waybillType").asInt(-1)==1 && value.matches("[A-Za-z0-9-]{1,64}")) return value;
        }
        throw new IllegalStateException("顺丰响应缺少主运单号");
    }
    private String safeResult(String external,String tracking,boolean cancelled) throws Exception {
        var result=new TreeMap<String,Object>();
        result.put("orderId",external); result.put("sandbox",true);
        if(tracking!=null) result.put("trackingNo",tracking);
        if(cancelled) result.put("cancelled",true);
        return mapper.writeValueAsString(result);
    }
}
