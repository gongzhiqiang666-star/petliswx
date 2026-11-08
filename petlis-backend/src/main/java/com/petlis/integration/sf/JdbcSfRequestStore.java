package com.petlis.integration.sf;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Each statement commits independently: never hold a business transaction across carrier HTTP. */
public final class JdbcSfRequestStore implements SfRequestCoordinator.Store {
    private final JdbcTemplate jdbc;
    public JdbcSfRequestStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public SfRequestCoordinator.Request reserve(int orderId,String operation,String key,String external,String hash,int actorId) {
        return reserveAttempt(orderId,operation,key,external,hash,actorId,SfRequestCoordinator.Attempt.first());
    }
    @Override public SfRequestCoordinator.Request reserveAttempt(int orderId,String operation,String key,String external,String hash,int actorId,SfRequestCoordinator.Attempt attempt) {
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("SF HTTP cannot run inside a business transaction");
        if(attempt==null || attempt.number()<1 || attempt.number()>10 || !java.util.Set.of("CREATE","CANCEL").contains(operation))throw new IllegalArgumentException("Invalid SF reservation");
        var transactions=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.support.JdbcTransactionManager(java.util.Objects.requireNonNull(jdbc.getDataSource())));
        transactions.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        return transactions.execute(status -> {
            var order=jdbc.queryForMap("SELECT o.SampleStatus,e.review_status FROM lin_lb_listdiagnosisproject o JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId WHERE o.ListDiagnosisProjectId=? FOR UPDATE",orderId);
            int number=attempt.number();
            if("CREATE".equals(operation)) {
                if(((Number)order.get("SampleStatus")).intValue()!=2 || !"approved".equals(order.get("review_status")))throw conflict("订单尚不满足寄件条件");
                if(number>1){
                    requireReshipPolicy(number,actorId);
                    if(attempt.previousCreateId()==null || attempt.previousCreateId()<=0 || attempt.reason()==null || attempt.reason().isBlank() || attempt.reason().length()>500)throw conflict("再次寄件需提供前次请求及原因");
                    var previous=jdbc.queryForList("SELECT id,attempt_no,external_order_no FROM lin_sf_request WHERE id=? AND order_id=? AND operation='CREATE' AND state='SUCCEEDED' FOR UPDATE",attempt.previousCreateId(),orderId);
                    if(previous.size()!=1 || ((Number)previous.get(0).get("attempt_no")).intValue()!=number-1)throw conflict("前次寄件请求已变化，请刷新核对");
                    if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lin_sf_request WHERE order_id=? AND operation='CREATE' AND attempt_no>?)",Boolean.class,orderId,number)))throw conflict("已有后续寄件，请查看当前请求");
                    if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lin_lab_task WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_report_workflow WHERE order_id=?) OR EXISTS(SELECT 1 FROM lin_audit_record WHERE resource_type='MOCK_REFUND' AND action='MOCK_REFUND_COMPLETED' AND resource_id=?)",Boolean.class,orderId,orderId,String.valueOf(orderId))))throw conflict("订单已进入检测、报告或退款，不能再次寄件");
                } else {
                    Integer shipments=jdbc.queryForObject("SELECT COUNT(*) FROM lin_shipment WHERE order_id=?",Integer.class,orderId);
                    Integer requests=jdbc.queryForObject("SELECT COUNT(*) FROM lin_sf_request WHERE order_id=? AND operation='CREATE'",Integer.class,orderId);
                    if(shipments!=null && shipments>0 && (requests==null || requests==0))throw conflict("已有人工运单，不得重复下单");
                }
            }
            var existing=jdbc.queryForList("SELECT id FROM lin_sf_request WHERE order_id=? AND attempt_no=? AND operation=? FOR UPDATE",orderId,number,operation);
            if(!existing.isEmpty())return read("id=?",existing.get(0).get("id"));
            if("CREATE".equals(operation) && number>1){
                var prior=jdbc.queryForList("SELECT s.id FROM lin_shipment s JOIN lin_sf_request c ON c.order_id=s.order_id AND c.attempt_no=s.attempt_no AND c.operation='CANCEL' AND c.state='SUCCEEDED' JOIN lin_sf_request r ON r.order_id=s.order_id AND r.attempt_no=s.attempt_no AND r.operation='CREATE' AND r.tracking_no=s.tracking_no AND r.state='SUCCEEDED' AND r.external_order_no=c.external_order_no WHERE s.order_id=? AND s.attempt_no=? AND s.is_current=1 AND s.status='CANCELLED' FOR UPDATE",orderId,number-1);
                if(prior.size()!=1)throw conflict("前次运单尚未确认取消，不能再次寄件");
            }
            if("CANCEL".equals(operation)){
                var shipment=jdbc.queryForList("SELECT s.status,r.external_order_no FROM lin_shipment s JOIN lin_sf_request r ON r.order_id=s.order_id AND r.attempt_no=s.attempt_no AND r.tracking_no=s.tracking_no AND r.operation='CREATE' AND r.state='SUCCEEDED' WHERE s.order_id=? AND s.is_current=1 AND s.attempt_no=? FOR UPDATE",orderId,number);
                if(shipment.size()!=1 || !external.equals(shipment.get(0).get("external_order_no")) || !java.util.Set.of("PENDING_PICKUP","CANCELLED").contains(String.valueOf(shipment.get(0).get("status"))))throw conflict("仅当前待揽收运单可申请取消");
            }
            jdbc.update("INSERT INTO lin_sf_request(order_id,attempt_no,operation,request_key,external_order_no,payload_hash,state,created_by) VALUES(?,?,?,?,?,?,'RESERVED',?)",orderId,number,operation,key,external,hash,actorId);
            var reserved=read("order_id=? AND attempt_no=? AND operation=?",orderId,number,operation);
            if(number>1 && "CREATE".equals(operation))jdbc.update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json,reason) VALUES(?,'SF_RESHIP_RESERVED','SF_REQUEST',?,?,JSON_OBJECT('attemptNo',?,'previousCreateId',?),?)",actorId,String.valueOf(reserved.id()),key,number,attempt.previousCreateId(),attempt.reason().trim());
            return reserved;
        });
    }
    private void requireReshipPolicy(int number,int actor){
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM lin_user_role r JOIN lin_user u ON u.id=r.user_id WHERE r.user_id=? AND r.role_code='ADMIN' AND r.is_active=1 AND u.status='normal')",Boolean.class,actor)))throw new com.petlis.common.exception.BusinessException(403,"需要平台管理员权限");
        var policy=new SfReshipPolicy(jdbc).load(true);
        if(!policy.enabled() || !policy.valid())throw conflict(policy.message());
        if(number>policy.maxAttempts())throw conflict("已达到允许的寄件次数");
    }
    private com.petlis.common.exception.BusinessException conflict(String message){return new com.petlis.common.exception.BusinessException(409,message);}
    @Override public SfRequestCoordinator.Request get(long id) { return read("id=?",id); }
    private SfRequestCoordinator.Request read(String predicate,Object...args) {
        return jdbc.queryForObject("SELECT id,operation,request_key,external_order_no,payload_hash,state,tracking_no,result_json FROM lin_sf_request WHERE "+predicate,
            (rs,row)->new SfRequestCoordinator.Request(rs.getLong("id"),rs.getString("operation"),rs.getString("request_key"),rs.getString("external_order_no"),rs.getString("payload_hash"),rs.getString("state"),rs.getString("tracking_no"),rs.getString("result_json")),args);
    }
    @Override public boolean claim(long id) {
        return jdbc.update("UPDATE lin_sf_request SET state='SENDING',version=version+1 WHERE id=? AND state='RESERVED'",id)==1;
    }
    @Override public void finish(long id,String state,String platform,String business,String tracking,String result) {
        if(!java.util.Set.of("SUCCEEDED","REJECTED","UNKNOWN").contains(state)) throw new IllegalArgumentException("非法请求状态");
        if(jdbc.update("UPDATE lin_sf_request SET state=?,platform_code=?,business_code=?,tracking_no=?,result_json=?,version=version+1 WHERE id=? AND state='SENDING'",state,platform,business,tracking,result,id)!=1)
            throw new IllegalStateException("顺丰请求状态已变化，请查单核对");
    }
    @Override public boolean reconcile(long id,String tracking,String result) {
        return jdbc.update("UPDATE lin_sf_request SET state='SUCCEEDED',tracking_no=?,result_json=?,platform_code='A1000',business_code='S0000',version=version+1 WHERE id=? AND operation='CREATE' AND state IN ('SENDING','UNKNOWN')",tracking,result,id)==1;
    }
    @Override public boolean reconcileCancellation(long id,String result) {
        return jdbc.update("UPDATE lin_sf_request SET state='SUCCEEDED',result_json=?,platform_code='A1000',business_code='8037',version=version+1 WHERE id=? AND operation='CANCEL' AND state IN ('SENDING','UNKNOWN')",result,id)==1;
    }
}
