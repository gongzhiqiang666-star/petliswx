package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Project/Pay.index persistence only. No network, settlement, wallet posting or scheduler. */
@Service
@RequiredArgsConstructor
public class LegacyBankPaymentPreparationService {
    private final FeatureStore store;
    private final AccessService access;
    private final LegacyBankPaymentQuoteService quotes;
    private static final String OPERATION = "LEGACY_BANK_PREPARE";
    private static final SecureRandom RANDOM = new SecureRandom();
    public record Command(String mode, Long hospital, String month, String orderNo,
                          String displayedAmount, String requestKey) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
    public record Prepared(String orderId, String amount, String amountFen, int count, String status) {}

    @Transactional(rollbackFor=Exception.class)
    public Prepared prepare(Command command, String merchant) {
        access.requireAdmin();
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new BusinessException(409,"支付单准备必须在事务内执行");
        if(command==null || command.requestKey()==null || !command.requestKey().matches("[A-Za-z0-9_-]{1,128}")
                || merchant==null || !merchant.matches("[A-Za-z0-9]{1,20}")
                || command.hospital()==null || command.hospital()<0 || command.hospital()>Integer.MAX_VALUE)
            throw new BusinessException(400,"支付请求参数无效");
        var configuration=store.one("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-bank-payments' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE' FOR UPDATE");
        try {
            var config=new ObjectMapper().readTree(configuration.get("config_value").toString());
            if(!config.path("enabled").isBoolean() || !config.path("enabled").asBoolean()
                    || !config.path("prepareEnabled").isBoolean() || !config.path("prepareEnabled").asBoolean())
                throw new BusinessException(409,"原网银支付单准备尚未开启");
        } catch(BusinessException e){throw e;} catch(Exception e){throw new BusinessException(409,"原网银业务参数无效");}
        int actor=access.current().userId();
        // Serializes this actor's new Java requests, not frozen PHP writers.
        store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
        String hash=hash(store.encode(command)+"\n"+merchant);
        var previous=store.jdbc().queryForList("SELECT request_hash,response_body,status FROM lin_order_idempotency WHERE user_id=? AND operation=? AND idempotency_key=? FOR UPDATE",actor,OPERATION,command.requestKey());
        if(!previous.isEmpty()) {
            var saved=previous.get(0);
            if(!hash.equals(saved.get("request_hash")))throw new BusinessException(409,"幂等键已用于不同的支付请求");
            try{var result=new ObjectMapper().readValue(saved.get("response_body").toString(),Prepared.class);return new Prepared(result.orderId(),result.amount(),result.amountFen(),result.count(),saved.get("status").toString());}
            catch(Exception e){throw new BusinessException(409,"原支付请求记录需人工核对，禁止重新创建");}
        }
        var quote=quotes.quote(command.mode(),command.hospital(),command.month(),command.orderNo());
        var items=items(quote);
        // Lock original clinical rows, then re-read eligibility/amount before allocating.
        var parents=new TreeSet<Long>();var details=new TreeSet<Long>();
        for(var item:items){parents.add(number(item,"ListDiagnosisProjectId"));details.add(number(item,"DiagnosisDetailId"));}
        for(long id:parents)store.one("SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? FOR UPDATE",id);
        for(long id:details)store.one("SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=? FOR UPDATE",id);
        var current=quotes.quoteLocked(command.mode(),command.hospital(),command.month(),command.orderNo());
        if(!store.encode(items).equals(store.encode(items(current))))throw new BusinessException(409,"待付项目发生变化，请重新核价");
        try {
            if(new BigDecimal(command.displayedAmount()).compareTo(new BigDecimal(current.get("displayAmount").toString()))!=0)
                throw new BusinessException(409,"金额不一致，请重新核价");
        } catch(NumberFormatException|NullPointerException e){throw new BusinessException(400,"核对金额格式无效");}
        LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0);
        boolean monthly="MONTH".equals(command.mode());
        String payMonth="",md5;
        if(monthly){var ym=YearMonth.parse(command.month());payMonth=ym.atDay(1)+" 00:00:00";md5=command.hospital()+"_"+ym.atDay(1)+"至"+ym.atEndOfMonth();}
        else md5=command.orderNo().trim()+"_"+String.join("_",details.stream().map(String::valueOf).toList());
        if(md5.length()>100)throw new BusinessException(409,"原支付关联字段容量不足，请拆分或核对后处理");
        String stem=merchant+now.format(DateTimeFormatter.ofPattern(monthly?"yyyyMMdd":"yyyyMMddHHmmss"))
                +(monthly?String.format(Locale.ROOT,"%06d",command.hospital()):"");
        String orderId=null;
        // Original suffix range retained; reject collisions rather than overwrite historical orders.
        for(int attempt=0;attempt<20;attempt++){
            String candidate=stem+(100+RANDOM.nextInt(900));
            if(store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_payordermain WHERE OrderId=?",Integer.class,candidate)==0){orderId=candidate;break;}
        }
        if(orderId==null)throw new BusinessException(409,"支付单号冲突，请稍后重试");
        String amount=current.get("amount").toString();
        store.insert("INSERT INTO lin_lb_payordermain(OrderId,Md5Id,CHKMoney,HostId,PayMonth,Remark,IsSuccess,AliTradeId,CreateDt,UpdateDt) VALUES(?,?,?,0,?,?,0,'',?,?)",
                orderId,md5,amount,payMonth,current.get("description"),now,now);
        for(var item:items)store.insert("INSERT INTO lin_lb_payorderdetail(MainOrderId,DetailId,PayMoney,ShowMoney,UserNo,CreateDt,UpdateDt) VALUES(?,?,?,?,?,?,?)",
                orderId,item.get("DiagnosisDetailId"),item.get("TestPrice"),item.get("TestPrice"),item.get("UserNO"),now,now);
        var result=new Prepared(orderId,amount,current.get("amountFen").toString(),items.size(),"READY");
        store.insert("INSERT INTO lin_order_idempotency(user_id,hospital_id,operation,idempotency_key,request_hash,response_code,response_body,status,expires_at) VALUES(?,?,?,?,?,200,?,'READY',?)",
                actor,command.hospital(),OPERATION,command.requestKey(),hash,store.encode(Map.of("orderId",result.orderId(),"amount",result.amount(),"amountFen",result.amountFen(),"count",result.count(),"status",result.status(),"merchant",merchant)),now.plusDays(1));
        store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,'LEGACY_BANK_PREPARED','LEGACY_BANK_PAYMENT',?,?,?)",
                actor,orderId,command.requestKey(),store.encode(Map.of("request",command,"result",result)));
        return result;
    }
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> items(Map<String,Object> quote){return (List<Map<String,Object>>)quote.get("items");}
    private static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
