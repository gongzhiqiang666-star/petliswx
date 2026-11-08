package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.*;
import java.util.*;

/** PHP finance/Lbpaylog is a business transaction register, not an accounting command. */
@Service @RequiredArgsConstructor
public class LegacyPaymentLogService {
 private final FeatureStore store;
 private final AccessService access;
 static final List<String> FIELDS=List.of("BuyMail","BuyerId","TradeNoAli","TradeNoUser","TotalFee","TradeStatus","Subject","TradeBody","Exterface","CreateDt");
 private static final String COLUMNS="PayLogId,"+String.join(",",FIELDS);
 private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
 public record Command(Map<String,String> fields,String requestKey,String expectedToken){}
 public record DeleteCommand(String requestKey,String expectedToken){}
 boolean[] policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-payment-logs' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));
  if(rows.size()!=1)return new boolean[]{false,false,false,false};
  try{var json=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=json.path("enabled").isBoolean()&&json.path("enabled").asBoolean(),write=read&&json.path("writeEnabled").isBoolean()&&json.path("writeEnabled").asBoolean();return new boolean[]{read,write,write&&json.path("deleteEnabled").isBoolean()&&json.path("deleteEnabled").asBoolean(),write&&json.path("importEnabled").isBoolean()&&json.path("importEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"交易流水参数无效");}
 }
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("paymentLogs",p[0],"paymentLogWrites",p[1],"paymentLogDeletes",p[2],"paymentLogImports",p[3]);}
 private void read(){if(!policy(false)[0])throw new BusinessException(409,"交易流水维护尚未开启");}
 private void id(long id){if(id<1||id>4294967295L)throw new BusinessException(400,"交易流水编号无效");}
 Map<String,Object> raw(long id,boolean lock){id(id);return new LinkedHashMap<>(store.one("SELECT "+COLUMNS+" FROM lin_lb_paylog WHERE PayLogId=?"+(lock?" FOR UPDATE":""),id));}
 String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 Map<String,Object> display(Map<String,Object> raw){var row=new LinkedHashMap<>(raw);row.put("TotalFee",((BigDecimal)raw.get("TotalFee")).toPlainString());Object date=raw.get("CreateDt");if(date instanceof java.sql.Timestamp t)date=t.toLocalDateTime();if(date instanceof LocalDateTime t)date=DATE.format(t);row.put("CreateDt",date);return row;}
 @Transactional(readOnly=true) public Map<String,Object> list(String q,int page,int size){
  read();if(page<0||size<1||size>100||q!=null&&q.length()>1000)throw new BusinessException(400,"交易流水查询无效");
  String term="%"+(q==null?"":q)+"%",where=" FROM lin_lb_paylog WHERE TradeNoAli LIKE ? OR TradeNoUser LIKE ? OR BuyerId LIKE ? OR Subject LIKE ?";
  var result=new LinkedHashMap<>(store.page("SELECT COUNT(*)"+where,"SELECT "+COLUMNS+where+" ORDER BY PayLogId DESC",page,size,term,term,term,term));
  @SuppressWarnings("unchecked") var rows=(List<Map<String,Object>>)result.get("items");result.put("items",rows.stream().map(this::display).toList());return result;
 }
 @Transactional(readOnly=true) public Map<String,Object> detail(long id){read();var raw=raw(id,false);var row=display(raw);row.put("editToken",token(raw));return row;}
 private void request(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"交易流水请求号无效");}
 private void version(String version){if(version==null||!version.matches("[a-f0-9]{64}"))throw new BusinessException(400,"请重新打开交易流水后修改");}
 private List<Object> values(Map<String,String> fields){
  if(fields==null||!fields.keySet().equals(new HashSet<>(FIELDS)))throw new BusinessException(400,"交易流水字段缺失或包含未知字段");var values=new ArrayList<Object>();
  for(String field:FIELDS){String v=fields.get(field);if(v==null)throw new BusinessException(400,"交易流水字段不能为null");
   if(field.equals("TotalFee")){if(!v.matches("[+-]?[0-9]{1,18}(\\.[0-9]{1,4})?"))throw new BusinessException(400,"交易金额须为最多四位小数的十进制数");BigDecimal money=new BigDecimal(v).setScale(2,RoundingMode.HALF_UP);if(money.abs().compareTo(new BigDecimal("9999999999999999.99"))>0)throw new BusinessException(400,"交易金额超出原字段范围");values.add(money);}
   else if(field.equals("CreateDt")){try{var date=LocalDateTime.parse(v,DATE);if(date.getYear()<1000||date.getYear()>9999)throw new IllegalArgumentException();values.add(date);}catch(Exception e){throw new BusinessException(400,"交易时间须为有效的年月日时分秒");}}
   else{int max=field.equals("TradeNoAli")||field.equals("TradeNoUser")?50:250;if(v.codePointCount(0,v.length())>max||v.indexOf('\0')>=0)throw new BusinessException(400,"交易流水文本过长或含无效字符");values.add(v);}
  }return values;
 }
 Map<String,String> normalized(Map<String,String> fields){var args=values(fields);var normalized=new TreeMap<String,String>();for(int i=0;i<FIELDS.size();i++){Object value=args.get(i);normalized.put(FIELDS.get(i),value instanceof LocalDateTime t?DATE.format(t):value instanceof BigDecimal m?m.toPlainString():value.toString());}return normalized;}
 private Long replay(int actor,String key,String canonical){var rows=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_PAYMENT_LOG' AND request_id=? ORDER BY id DESC LIMIT 1",actor,key);if(rows.isEmpty())return null;try{var mapper=new ObjectMapper();var data=mapper.readTree(rows.get(0).get("after_json").toString());if(!mapper.readTree(canonical).equals(data.path("canonicalRequest")))throw new BusinessException(409,"请求号已用于其他流水操作");return data.path("paymentLogId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"交易流水请求无法核对");}}
 private void audit(int actor,String action,long id,String key,Map<String,Object> before,Map<String,Object> canonical){store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,?,'LEGACY_PAYMENT_LOG',?,?,?,?)",actor,action,String.valueOf(id),key,before==null?null:store.encode(before),store.encode(Map.of("paymentLogId",id,"canonicalRequest",canonical)));}
 @Transactional public long save(Long id,Command c){
  if(!policy(true)[1])throw new BusinessException(409,"交易流水写入尚未开启");if(c==null)throw new BusinessException(400,"交易流水请求缺失");var args=values(c.fields());request(c.requestKey());if(id!=null){id(id);version(c.expectedToken());}int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  Map<String,Object> canonical=Map.of("operation",id==null?"CREATE":"UPDATE","id",id==null?0:id,"command",c);var prior=replay(actor,c.requestKey(),store.encode(canonical));if(prior!=null)return prior;
  long result;Map<String,Object> before=null;
  if(id==null)result=store.insert("INSERT INTO lin_lb_paylog("+String.join(",",FIELDS)+") VALUES(?,?,?,?,?,?,?,?,?,?)",args.toArray());
  else{before=raw(id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"交易流水已变更，请重新打开核对");args.add(id);store.jdbc().update("UPDATE lin_lb_paylog SET "+String.join(",",FIELDS.stream().map(f->f+"=?").toList())+" WHERE PayLogId=?",args.toArray());result=id;}
  audit(actor,id==null?"LEGACY_PAYMENT_LOG_CREATED":"LEGACY_PAYMENT_LOG_UPDATED",result,c.requestKey(),before,canonical);return result;
 }
 @Transactional public long delete(long id,DeleteCommand c){
  if(!policy(true)[2])throw new BusinessException(409,"交易流水删除尚未开启");id(id);if(c==null)throw new BusinessException(400,"删除请求缺失");request(c.requestKey());version(c.expectedToken());int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  Map<String,Object> canonical=Map.of("operation","DELETE","id",id,"command",c);var prior=replay(actor,c.requestKey(),store.encode(canonical));if(prior!=null)return prior;
  var before=raw(id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"交易流水已变更，请重新打开核对");store.jdbc().update("DELETE FROM lin_lb_paylog WHERE PayLogId=?",id);audit(actor,"LEGACY_PAYMENT_LOG_DELETED",id,c.requestKey(),before,canonical);return id;
 }
}
