package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import com.petlis.integration.sf.*;
import org.springframework.stereotype.Service;
import java.util.*;

/** Sandbox business adapter: no production endpoint or credentials are accepted. */
@Service
public class SfShipmentService {
    private final FeatureStore store;
    private final AccessService access;
    private final ShipmentWorkflowService shipments;
    private final ObjectMapper mapper;
    private final SfSandboxConfiguration configuration;
    private final SfRouteEventService routes;
    public SfShipmentService(FeatureStore store,AccessService access,ShipmentWorkflowService shipments,
                             ObjectMapper mapper,SfRouteEventService routes,SfSandboxConfiguration configuration) {
        this.store=store;this.access=access;this.shipments=shipments;this.mapper=mapper;this.routes=routes;this.configuration=configuration;
    }
    public record Contact(String contact,String mobile,String province,String city,String county,String address) {}
    public record CreateCommand(Contact sender,Contact recipient) {}

    public Map<String,Object> create(int orderId,CreateCommand command) {
        access.requireAdmin();
        eligible(orderId);
        if(!store.jdbc().queryForList("SELECT id FROM lin_sf_request WHERE order_id=? AND operation='CANCEL' AND state<>'REJECTED'",orderId).isEmpty())
            throw new BusinessException(409,"该运单已申请取消，请查看取消结果，不能再次寄件");
        var payload=payload(command);
        try {
            var request=coordinator().dispatch(orderId,access.current().userId(),"CREATE",payload);
            return link(orderId,request,command.sender().address());
        } catch(BusinessException e) { throw e; }
        catch(Exception e) { throw new BusinessException(409,"顺丰请求未完成，请查询请求状态，勿重复寄件"); }
    }
    public record ReshipCommand(Long previousCreateRequestId,String reason,Contact sender,Contact recipient) {}
    public Map<String,Object> reship(int orderId,ReshipCommand command) {
        access.requireAdmin();eligible(orderId);
        if(command==null || command.previousCreateRequestId()==null || command.previousCreateRequestId()<=0 || command.reason()==null || command.reason().isBlank() || command.reason().length()>500)throw new BusinessException(400,"请提供前次请求及再次寄件原因");
        var policy=new SfReshipPolicy(store.jdbc()).load(false);if(!policy.enabled() || !policy.valid())throw new BusinessException(409,policy.message());
        var previous=store.one("SELECT attempt_no FROM lin_sf_request WHERE id=? AND order_id=? AND operation='CREATE' AND state='SUCCEEDED'",command.previousCreateRequestId(),orderId);
        int attempt=((Number)previous.get("attempt_no")).intValue()+1;if(attempt>10)throw new BusinessException(409,"已达到最大寄件次数");
        try{var request=coordinator().dispatch(orderId,access.current().userId(),"CREATE",payload(new CreateCommand(command.sender(),command.recipient())),new SfRequestCoordinator.Attempt(attempt,command.previousCreateRequestId(),command.reason().trim()));return link(orderId,request,command.sender().address());}
        catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(409,"再次寄件结果待核对，请查询原请求，不要重复下单");}
    }
    public Map<String,Object> reshipOptions(int orderId) {
        access.requireAdmin();var order=authorizeOrder(orderId);var policy=new SfReshipPolicy(store.jdbc()).load(false);var options=new LinkedHashMap<String,Object>();options.put("enabled",policy.enabled());options.put("maxAttempts",policy.maxAttempts());options.put("canReship",false);options.put("reason",policy.message());
        if(!policy.enabled())return options;
        var requests=store.jdbc().queryForList("SELECT id,attempt_no,state,tracking_no,external_order_no FROM lin_sf_request WHERE order_id=? AND operation='CREATE' ORDER BY attempt_no DESC LIMIT 1",orderId);
        if(requests.isEmpty()){options.put("reason","尚无可再次寄件的顺丰运单");return options;}
        var request=requests.get(0);int attempt=((Number)request.get("attempt_no")).intValue();options.put("previousCreateRequestId",request.get("id"));options.put("attemptNo",attempt);options.put("nextAttemptNo",attempt+1);
        String reason="";
        if(attempt>=policy.maxAttempts())reason="已达到允许的寄件次数";
        else if(!"SUCCEEDED".equals(request.get("state")))reason="当前请求结果未确认，请先查询原请求";
        else if(!"approved".equals(order.get("review_status")) || ((Number)order.get("SampleStatus")).intValue()!=2)reason="送检单需审核通过并完成付款或月结挂账";
        else if(!Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_shipment s JOIN lin_sf_request c ON c.order_id=s.order_id AND c.attempt_no=s.attempt_no AND c.operation='CANCEL' AND c.state='SUCCEEDED' WHERE s.order_id=? AND s.attempt_no=? AND s.tracking_no=? AND s.is_current=1 AND s.status='CANCELLED' AND c.external_order_no=?)",Boolean.class,orderId,attempt,request.get("tracking_no"),request.get("external_order_no"))))reason="前次运单尚未确认取消，请先核对取消结果";
        else if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_lab_task WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_report_workflow WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_audit_record WHERE resource_type='MOCK_REFUND' AND action='MOCK_REFUND_COMPLETED' AND resource_id=?)",Boolean.class,orderId,orderId,String.valueOf(orderId))))reason="订单已进入检测、报告或退款，不能再次寄件";
        options.put("canReship",reason.isEmpty());options.put("reason",reason);return options;
    }
    private Map<String,Object> payload(CreateCommand command) {
        if(command==null) throw new BusinessException(400,"请填写寄件和收件信息");
        var sender=contact(command.sender(),1);
        var recipient=contact(command.recipient(),2);
        var payload=new LinkedHashMap<String,Object>();
        payload.put("language","zh-CN"); payload.put("expressTypeId",1);payload.put("payMethod",1);
        // Sandbox orders never request a courier pickup.
        payload.put("isDocall",0);payload.put("parcelQty",1);payload.put("totalWeight",new java.math.BigDecimal("0.1"));
        payload.put("cargoDetails",List.of(Map.of("name","接口联调测试件","count",1,"unit","件")));
        payload.put("contactInfoList",List.of(sender,recipient));
        return payload;
    }
    public Map<String,Object> status(int orderId) {
        authorizeOrder(orderId);
        var rows=store.jdbc().queryForList("SELECT r.id,r.attempt_no AS attemptNo,r.state,r.tracking_no AS trackingNo,r.platform_code AS platformCode,r.business_code AS businessCode,r.updated_at AS updatedAt,(SELECT s.id FROM lin_shipment s WHERE s.order_id=r.order_id AND s.attempt_no=r.attempt_no AND s.tracking_no=r.tracking_no) AS shipmentId FROM lin_sf_request r WHERE r.order_id=? AND r.operation='CREATE' ORDER BY r.attempt_no DESC LIMIT 1",orderId);
        if(rows.isEmpty()) return Map.of("state","NOT_CREATED","sandbox",true);
        var result=new LinkedHashMap<String,Object>(rows.get(0));result.put("sandbox",true);
        var cancellation=store.jdbc().queryForList("SELECT id,state FROM lin_sf_request WHERE order_id=? AND attempt_no=? AND operation='CANCEL'",orderId,result.get("attemptNo"));
        if(!cancellation.isEmpty()) {result.put("cancellationState",cancellation.get(0).get("state"));result.put("cancellationRequestId",cancellation.get(0).get("id"));}
        return result;
    }
    public Map<String,Object> reconcile(int orderId) {
        access.requireAdmin();
        var current=status(orderId);
        if(Set.of("SENDING","UNKNOWN").contains(String.valueOf(current.get("cancellationState")))
            && current.get("cancellationRequestId") instanceof Number requestId && current.get("shipmentId") instanceof Number shipmentId) {
            try {
                var request=coordinator().reconcileCancellation(requestId.longValue());
                if("SUCCEEDED".equals(request.state())) shipments.applySfCancellation(shipmentId.longValue());
                return status(orderId);
            } catch(BusinessException e) {throw e;}
            catch(Exception e) {throw new BusinessException(409,"顺丰尚未确认取消，请保留当前请求，不要重复寄件");}
        }
        if ("SUCCEEDED".equals(current.get("cancellationState")) && current.get("shipmentId") instanceof Number shipmentId) {
            // Carrier confirmation is already durable. Repair only the local state; never resend cancellation.
            shipments.applySfCancellation(shipmentId.longValue());
            return status(orderId);
        }
        if(current.get("shipmentId")!=null) return current;
        eligible(orderId);
        var row=current;
        try {
            var request=coordinator().reconcileCreate(((Number)row.get("id")).longValue());
            return link(orderId,request,null);
        } catch(BusinessException e) { throw e; }
        catch(Exception e) { throw new BusinessException(409,"查单尚未确认主运单，请保留当前请求，不要重新下单"); }
    }
    public Map<String,Object> cancel(int orderId) {
        access.requireAdmin();authorizeOrder(orderId);
        var shipment=store.one("SELECT s.id,s.status,s.attempt_no FROM lin_shipment s JOIN lin_sf_request r ON r.order_id=s.order_id AND r.attempt_no=s.attempt_no AND r.tracking_no=s.tracking_no AND r.operation='CREATE' AND r.state='SUCCEEDED' WHERE s.order_id=? AND s.is_current=1",orderId);
        if(!Set.of("PENDING_PICKUP","CANCELLED").contains(String.valueOf(shipment.get("status"))))
            throw new BusinessException(409,"仅待揽收的顺丰运单可取消");
        try {
            var request=coordinator().dispatch(orderId,access.current().userId(),"CANCEL",Map.of(),new SfRequestCoordinator.Attempt(((Number)shipment.get("attempt_no")).intValue(),null,""));
            if("SUCCEEDED".equals(request.state())) shipments.applySfCancellation(((Number)shipment.get("id")).longValue());
            return status(orderId);
        } catch(BusinessException e) {throw e;}
        catch(Exception e) {throw new BusinessException(409,"取消结果尚未确认，请查询请求状态，勿重复操作");}
    }
    public Map<String,Object> refreshRoutes(int orderId) {
        access.requireAdmin();authorizeOrder(orderId);
        var shipment=store.one("SELECT s.id,s.tracking_no FROM lin_shipment s JOIN lin_sf_request r ON r.order_id=s.order_id AND r.attempt_no=s.attempt_no AND r.tracking_no=s.tracking_no AND r.operation='CREATE' AND r.state='SUCCEEDED' WHERE s.order_id=? AND s.is_current=1",orderId);
        try {
            // Query by the linked waybill, not by an arbitrary client-provided number.
            var outcome=gateway().call("EXP_RECE_SEARCH_ROUTES",Map.of("language","0","trackingType","1","trackingNumber",List.of(shipment.get("tracking_no")),"methodType","1"),UUID.randomUUID().toString(),System.currentTimeMillis());
            if(!outcome.success()) throw new BusinessException(409,"顺丰暂未返回路由，请稍后查询");
            routes.applyVerifiedBatch(((Number)shipment.get("id")).longValue(),String.valueOf(shipment.get("tracking_no")),outcome.data());
            return shipments.get(((Number)shipment.get("id")).longValue());
        } catch(BusinessException e) {throw e;}
        catch(Exception e) {throw new BusinessException(409,"顺丰路由查询暂未完成，请稍后重试");}
    }
    private Map<String,Object> link(int orderId,SfRequestCoordinator.Request request,String pickupAddress) {
        if(!"SUCCEEDED".equals(request.state())) return status(orderId);
        long id=shipments.linkSfRequest(orderId,request.id(),pickupAddress);
        var result=new LinkedHashMap<String,Object>(status(orderId));result.put("shipmentId",id);return result;
    }
    private Map<String,Object> eligible(int orderId) {
        var order=authorizeOrder(orderId);
        if(!"approved".equals(order.get("review_status")) || ((Number)order.get("SampleStatus")).intValue()!=2)
            throw new BusinessException(409,"送检单需审核通过并完成付款或月结挂账后才能寄件");
        return order;
    }
    private Map<String,Object> authorizeOrder(int orderId) {
        var order=store.one("SELECT o.InfoHospitalId,o.SampleStatus,e.review_status,e.laboratory_id FROM lin_lb_listdiagnosisproject o JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId WHERE o.ListDiagnosisProjectId=?",orderId);
        access.requireHospital(((Number)order.get("InfoHospitalId")).intValue());return order;
    }
    private Map<String,Object> contact(Contact c,int type) {
        if(c==null || !valid(c.contact(),100) || c.mobile()==null || !c.mobile().matches("1[3-9][0-9]{9}") || !valid(c.province(),50) || !valid(c.city(),50) || !valid(c.county(),50) || !valid(c.address(),200))
            throw new BusinessException(400,"请完整填写姓名、有效手机号及省市区和详细地址");
        return Map.of("contactType",type,"contact",c.contact().trim(),"mobile",c.mobile(),"country","CN","province",c.province().trim(),"city",c.city().trim(),"county",c.county().trim(),"address",c.address().trim());
    }
    private boolean valid(String s,int max) {return s!=null && !s.isBlank() && s.length()<=max;}
    private SfRequestCoordinator coordinator() {
        return new SfRequestCoordinator(new JdbcSfRequestStore(store.jdbc()),gateway(),mapper);
    }
    private SfSandboxGateway gateway() { return configuration.load().gateway(); }
}
