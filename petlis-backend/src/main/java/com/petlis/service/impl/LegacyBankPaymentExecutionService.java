package com.petlis.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.*;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Durable state before/after network. All settlement writes follow Project/Pay.query direct SQL. */
@Service @RequiredArgsConstructor
public class LegacyBankPaymentExecutionService {
 private final FeatureStore store;private final AccessService access;private final ObjectMapper json;
 private static final String OP="LEGACY_BANK_PREPARE";
 private static final SecureRandom RANDOM=new SecureRandom();
 public record Claimed(String orderId,String token,Map<String,Object> parameters){}
 @Transactional(readOnly=true) public boolean allowed(String flag){
  access.requireAdmin();return enabled(flag);
 }
 private boolean enabled(String flag){var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-bank-payments' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");
  if(rows.size()!=1)return false;
  try{var config=json.readTree(rows.get(0).get("config_value").toString());return config.path("enabled").isBoolean()&&config.path("enabled").asBoolean()&&config.path(flag).isBoolean()&&config.path(flag).asBoolean();}
  catch(Exception e){throw new BusinessException(409,"原网银业务参数无效");}
 }
 @Transactional(rollbackFor=Exception.class)
 public Claimed claim(String key,String merchant,boolean query){
  requireTransaction();lockConfiguration();if(!allowed(query?"queryEnabled":"sendEnabled"))throw new BusinessException(409,"网银发送或查询尚未开启");
  var row=row(key);var body=body(row);String status=row.get("status").toString();
  if(!Objects.equals(merchant,body.get("merchant")))throw new BusinessException(409,"支付请求商户不一致或旧记录缺少身份，请人工核对");
  if("SUCCEEDED".equals(status))return null;
  if(!query && "CASHIER_READY".equals(status))return null;
  if(!query && !"READY".equals(status))throw new BusinessException(409,"原请求已发送或结果待核对，禁止重复发送，请查询原交易");
  if(query && "READY".equals(status))throw new BusinessException(409,"该请求尚未发送银行");
  if(query && "QUERYING".equals(status) && row.get("locked_until")!=null && timestamp(row.get("locked_until")).isAfter(LocalDateTime.now()))throw new BusinessException(409,"原交易正在查询，请稍后刷新");
  String orderId=text(body,"orderId");var main=main(orderId);var allocations=allocations(orderId);
  if(!query){
   if(!main.get("AliTradeId").toString().isEmpty())throw new BusinessException(409,"原单已有支付结果，请查询核对");
   BigDecimal allocated=BigDecimal.ZERO;
   for(var allocation:allocations){var detail=store.one("SELECT PayStatus2,IsCheckPass,TestPrice FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=? FOR UPDATE",allocation.get("DetailId"));
    if(((Number)detail.get("PayStatus2")).intValue()!=0||((Number)detail.get("IsCheckPass")).intValue()!=0||decimal(detail.get("TestPrice")).compareTo(decimal(allocation.get("PayMoney")))!=0)throw new BusinessException(409,"待付明细状态或金额已变化，请先核对原单");
    allocated=allocated.add(decimal(allocation.get("PayMoney"))).setScale(2,RoundingMode.DOWN);
   }
   if(allocations.isEmpty()||allocated.compareTo(decimal(main.get("CHKMoney")))!=0||allocated.compareTo(decimal(body.get("amount")))!=0||allocated.movePointRight(2).compareTo(decimal(body.get("amountFen")))!=0)throw new BusinessException(409,"支付分摊与原金额不一致，请人工核对");
  }
  String token=UUID.randomUUID().toString();body.put("executionToken",token);body.put("status",query?"QUERYING":"SENDING");
  body.put(query?"queryAttempts":"sendAttempts",((Number)body.getOrDefault(query?"queryAttempts":"sendAttempts",0)).intValue()+1);
  store.jdbc().update("UPDATE lin_order_idempotency SET status=?,response_body=?,locked_until=DATE_ADD(NOW(6),INTERVAL 70 SECOND) WHERE id=?",body.get("status"),store.encode(body),row.get("id"));audit(orderId,key,body.get("status").toString());
  var p=new LinkedHashMap<String,Object>();
  if(query){String now=LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));p.put("merOrdrNo",merchant+now+(100+RANDOM.nextInt(900)));p.put("oriMerOrdrNo",orderId);p.put("oriTrxDt",now);}
  else{p.put("merOrdrNo",orderId);p.put("trxTtlAmt",body.get("amountFen"));p.put("bizType","14900");p.put("trxChnlType","01");p.put("subject",main.get("Remark"));p.put("ordrDesc",main.get("Remark"));p.put("userNo",((Number)row.get("hospital_id")).intValue());p.put("userType","USER_ID");p.put("bizFunc","111011");}
  return new Claimed(orderId,token,Collections.unmodifiableMap(p));
 }
 @Transactional(rollbackFor=Exception.class) public void unknown(String key,String token){requireTransaction();lockConfiguration();var row=row(key);var body=body(row);if(!Objects.equals(token,body.get("executionToken"))||"SUCCEEDED".equals(row.get("status")))return;save(row,body,"UNKNOWN",key);}
 @Transactional(rollbackFor=Exception.class) public void finish(String key,String token,boolean query,JsonNode response){
  requireTransaction();lockConfiguration();var row=row(key);var body=body(row);
  if(!Objects.equals(token,body.get("executionToken"))||!Objects.equals(row.get("status"),query?"QUERYING":"SENDING"))return;
  String code=response.path("retCode").asText("");body.put("lastProviderCode",code.substring(0,Math.min(code.length(),64)));
  if(!query){String url=response.path("chnlUrl").asText("");if("00000".equals(code)&&safeCashier(url)){body.put("cashierUrl",url);save(row,body,"CASHIER_READY",key);}else save(row,body,code.isEmpty()||"00000".equals(code)?"UNKNOWN":"PROVIDER_REJECTED",key);return;}
  body.put("lastProviderTradeStatus",response.path("tradeStatus").asText("").substring(0,Math.min(32,response.path("tradeStatus").asText("").length())));
  if(!"00000".equals(code)||!"00".equals(response.path("tradeStatus").asText())){save(row,body,"UNKNOWN",key);return;}
  settle(row,body,key,response,"subOpenId",false);
 }
 @Transactional(rollbackFor=Exception.class)
 public boolean notification(JsonNode response,String merchant){
  requireTransaction();lockConfiguration();if(!enabled("notifyEnabled"))throw new BusinessException(409,"原网银通知尚未开启");
  if(!"00".equals(response.path("tradeStatus").asText()))return false;
  String order=response.path("oriMerOrdrNo").asText("");if(merchant==null||order.length()>50||!order.startsWith(merchant)||order.length()<=merchant.length())return false;
  var rows=store.jdbc().queryForList("SELECT * FROM lin_order_idempotency WHERE operation=? AND JSON_UNQUOTE(JSON_EXTRACT(response_body,'$.orderId'))=? FOR UPDATE",OP,order);
  if(rows.size()!=1)return false;var row=rows.get(0);var body=body(row);
  if(!Objects.equals(merchant,body.get("merchant"))||!(body.get("sendAttempts") instanceof Number attempts)||attempts.intValue()<1)return false;
  if("SUCCEEDED".equals(row.get("status"))){
   String fen=response.path("trxTtlAmt").asText("");return fen.matches("[0-9]{1,30}")&&new BigInteger(fen).compareTo(new BigInteger(text(body,"amountFen")))==0&&Objects.equals(body.get("providerTradeId"),response.path("chnlMerOrdrNo").asText());
  }
  // Invalidates any older in-flight query/send result; both paths share the same durable row lock.
  body.put("executionToken",UUID.randomUUID().toString());
  settle(row,body,row.get("idempotency_key").toString(),response,"buyerLgnId",true);
  return "SUCCEEDED".equals(body.get("status"));
 }
 private void settle(Map<String,Object> row,Map<String,Object> body,String key,JsonNode response,String buyerField,boolean notification){
  String orderId=text(body,"orderId"),channel=response.path("chnlMerOrdrNo").asText(""),fen=response.path("trxTtlAmt").asText("");
  if(!orderId.equals(response.path("oriMerOrdrNo").asText())||channel.isBlank()||channel.length()>50||!fen.matches("[0-9]{1,30}")||new BigInteger(fen).compareTo(new BigInteger(text(body,"amountFen")))!=0){body.put("validationFailure","ORDER_AMOUNT_OR_CHANNEL");save(row,body,"UNKNOWN",key,notification?"LEGACY_BANK_CALLBACK":"LEGACY_BANK_EXECUTION");return;}
  var main=main(orderId);var amount=new BigDecimal(fen).movePointLeft(2).setScale(2);var legacyFee=legacyLogAmount(fen);
  if(amount.compareTo(decimal(main.get("CHKMoney")))!=0){body.put("validationFailure","MAIN_AMOUNT");save(row,body,"UNKNOWN",key,notification?"LEGACY_BANK_CALLBACK":"LEGACY_BANK_EXECUTION");return;}
  var allocations=allocations(orderId);
  if(!main.get("AliTradeId").toString().isEmpty()){
   // Frozen PHP may already have processed it. Require its full receipt, never add a second log.
   var logs=store.jdbc().queryForList("SELECT TotalFee,TradeStatus FROM lin_lb_paylog WHERE TradeNoUser=? AND TradeNoAli=?",orderId,channel);
   if(!channel.equals(main.get("AliTradeId"))||logs.size()!=1||legacyFee.compareTo(decimal(logs.get(0).get("TotalFee")))!=0||!"TRADE_SUCCESS".equals(logs.get(0).get("TradeStatus"))){body.put("validationFailure","EXISTING_RECEIPT");save(row,body,"UNKNOWN",key,notification?"LEGACY_BANK_CALLBACK":"LEGACY_BANK_EXECUTION");return;}
  }else{
   if(!store.jdbc().queryForList("SELECT PayLogId FROM lin_lb_paylog WHERE TradeNoUser=?",orderId).isEmpty()){body.put("validationFailure","EXISTING_LOG_WITHOUT_RECEIPT");save(row,body,"UNKNOWN",key,notification?"LEGACY_BANK_CALLBACK":"LEGACY_BANK_EXECUTION");return;}
   var parents=new TreeSet<Long>();var clinical=new ArrayList<Map<String,Object>>();
   for(var allocation:allocations){var d=store.one("SELECT DiagnosisDetailId,ListDiagnosisProjectId,PayStatus2 FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=? FOR UPDATE",allocation.get("DetailId"));clinical.add(d);parents.add(((Number)d.get("ListDiagnosisProjectId")).longValue());}
   for(long parent:parents)store.one("SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? FOR UPDATE",parent);
   for(var d:clinical)if(((Number)d.get("PayStatus2")).intValue()==0){store.jdbc().update("UPDATE lin_lb_diagnosisdetail SET PayStatus2=3 WHERE DiagnosisDetailId=? AND PayStatus2=0",d.get("DiagnosisDetailId"));store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET IsPayMoney=3 WHERE ListDiagnosisProjectId=?",d.get("ListDiagnosisProjectId"));}
   if(store.jdbc().update("UPDATE lin_lb_payordermain SET AliTradeId=? WHERE IID=? AND AliTradeId=''",channel,main.get("IID"))!=1)throw new BusinessException(409,"原支付结果变化，请重新查询核对");
   String buyer=response.path(buyerField).asText("");if(buyer.length()>250)throw new BusinessException(409,"银行买方信息超出原字段容量");
   store.insert("INSERT INTO lin_lb_paylog(Subject,TradeBody,TradeNoUser,TradeNoAli,TotalFee,Exterface,BuyerId,BuyMail,TradeStatus,CreateDt) VALUES(?,?,?,?,?,'',?,'','TRADE_SUCCESS',?)",main.get("Remark"),main.get("Remark"),orderId,channel,legacyFee,buyer,LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
  }
  body.put("providerTradeId",channel);body.remove("validationFailure");save(row,body,"SUCCEEDED",key,notification?"LEGACY_BANK_CALLBACK":"LEGACY_BANK_EXECUTION");
 }
 private void lockConfiguration(){store.jdbc().queryForList("SELECT config_key FROM lin_operational_config WHERE config_key='legacy-bank-payments' AND scope_type='GLOBAL' AND scope_id=0 FOR UPDATE");}
 private Map<String,Object> row(String key){access.requireAdmin();if(key==null||!key.matches("[A-Za-z0-9_-]{1,128}"))throw new BusinessException(400,"支付请求编号无效");return store.one("SELECT * FROM lin_order_idempotency WHERE user_id=? AND operation=? AND idempotency_key=? FOR UPDATE",access.current().userId(),OP,key);}
 private Map<String,Object> main(String id){var rows=store.jdbc().queryForList("SELECT * FROM lin_lb_payordermain WHERE OrderId=? FOR UPDATE",id);if(rows.size()!=1)throw new BusinessException(409,"原支付单不存在或编号重复，请人工核对");return rows.get(0);}
 private List<Map<String,Object>> allocations(String id){return store.jdbc().queryForList("SELECT * FROM lin_lb_payorderdetail WHERE MainOrderId=? ORDER BY DetailId,IID FOR UPDATE",id);}
 private Map<String,Object> body(Map<String,Object> row){try{return json.readValue(row.get("response_body").toString(),new TypeReference<LinkedHashMap<String,Object>>(){});}catch(Exception e){throw new BusinessException(409,"支付请求记录格式异常，请人工核对");}}
 private void save(Map<String,Object> row,Map<String,Object> body,String status,String key){save(row,body,status,key,"LEGACY_BANK_EXECUTION");}
 private void save(Map<String,Object> row,Map<String,Object> body,String status,String key,String action){body.put("status",status);store.jdbc().update("UPDATE lin_order_idempotency SET status=?,response_body=?,locked_until=NULL,completed_at=CASE WHEN ?='SUCCEEDED' THEN NOW(6) ELSE completed_at END WHERE id=?",status,store.encode(body),status,row.get("id"));store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,?,'LEGACY_BANK_PAYMENT',?,?,?)",row.get("user_id"),action,text(body,"orderId"),key,store.encode(Map.of("status",status,"source",action)));}
 private void audit(String order,String key,String status){store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,'LEGACY_BANK_EXECUTION','LEGACY_BANK_PAYMENT',?,?,?)",access.current().userId(),order,key,store.encode(Map.of("status",status)));}
 // Source formats sprintf("%.2f", trxTtlAmt / 100): two IEEE operations, not exact decimal division.
 // Only the legacy log representation uses this; validation and all fund arithmetic stay decimal/integer.
 static BigDecimal legacyLogAmount(String fen){return new BigDecimal(new BigInteger(fen).doubleValue()/100.0).setScale(2,RoundingMode.HALF_EVEN);}
 private static String text(Map<String,Object> value,String key){Object v=value.get(key);if(v==null)throw new BusinessException(409,"原支付请求缺少必要字段");return v.toString();}
 private static BigDecimal decimal(Object v){return new BigDecimal(v.toString());}
 private static LocalDateTime timestamp(Object v){if(v instanceof Timestamp t)return t.toLocalDateTime();if(v instanceof LocalDateTime t)return t;return LocalDateTime.parse(v.toString().replace(' ','T'));}
 private static boolean safeCashier(String value){try{var uri=URI.create(value);return value.length()<=2000&&uri.getHost()!=null&&uri.getUserInfo()==null&&uri.getFragment()==null&&("https".equals(uri.getScheme())||"http".equals(uri.getScheme())&&"127.0.0.1".equals(uri.getHost())&&uri.getPort()>0);}catch(Exception e){return false;}}
 private static void requireTransaction(){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new BusinessException(409,"支付状态操作需要事务");}
}
