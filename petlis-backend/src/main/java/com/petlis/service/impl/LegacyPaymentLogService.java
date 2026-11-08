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
  if(rows.size()!=1)return new boolean[]{false,false,false,false,false,false};
  try{var json=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=json.path("enabled").isBoolean()&&json.path("enabled").asBoolean(),write=read&&json.path("writeEnabled").isBoolean()&&json.path("writeEnabled").asBoolean();return new boolean[]{read,write,write&&json.path("deleteEnabled").isBoolean()&&json.path("deleteEnabled").asBoolean(),write&&json.path("importEnabled").isBoolean()&&json.path("importEnabled").asBoolean(),write&&json.path("batchEnabled").isBoolean()&&json.path("batchEnabled").asBoolean(),read&&json.path("exportEnabled").isBoolean()&&json.path("exportEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"交易流水参数无效");}
 }
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("paymentLogs",p[0],"paymentLogWrites",p[1],"paymentLogDeletes",p[2],"paymentLogImports",p[3],"paymentLogBatches",p[4],"paymentLogExports",p[5]);}
 private void read(){if(!policy(false)[0])throw new BusinessException(409,"交易流水维护尚未开启");}
 private void id(long id){if(id<1||id>4294967295L)throw new BusinessException(400,"交易流水编号无效");}
 Map<String,Object> raw(long id,boolean lock){id(id);return new LinkedHashMap<>(store.one("SELECT "+COLUMNS+" FROM lin_lb_paylog WHERE PayLogId=?"+(lock?" FOR UPDATE":""),id));}
 String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 Map<String,Object> display(Map<String,Object> raw){var row=new LinkedHashMap<>(raw);row.put("TotalFee",((BigDecimal)raw.get("TotalFee")).toPlainString());Object date=raw.get("CreateDt");if(date instanceof java.sql.Timestamp t)date=t.toLocalDateTime();if(date instanceof LocalDateTime t)date=DATE.format(t);row.put("CreateDt",date);return row;}
 @Transactional(readOnly=true) public Map<String,Object> list(String q,int page,int size){return list(Map.of("q",q==null?"":q),page,size);}
 @Transactional(readOnly=true) public Map<String,Object> list(Map<String,String> query,int page,int size){
  read();if(page<0||size<1||size>100)throw new BusinessException(400,"交易流水查询无效");var plan=LegacyPaymentLogQuery.build(query);
  var result=new LinkedHashMap<>(store.page("SELECT COUNT(*)"+plan.where(),"SELECT "+COLUMNS+plan.where()+plan.order(),page,size,plan.args().toArray()));
  @SuppressWarnings("unchecked") var rows=(List<Map<String,Object>>)result.get("items");result.put("items",rows.stream().map(this::display).toList());return result;
 }
 @Transactional(readOnly=true) public byte[] export(Map<String,String> query){
  if(!policy(false)[5])throw new BusinessException(409,"交易流水导出尚未开启");var plan=LegacyPaymentLogQuery.build(query);var rows=store.jdbc().queryForList("SELECT "+COLUMNS+plan.where()+plan.order(),plan.args().toArray());
  try(var workbook=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var output=new java.io.ByteArrayOutputStream()){
   var sheet=workbook.createSheet("交易流水");var columns=new ArrayList<String>();columns.add("PayLogId");columns.addAll(FIELDS);var header=sheet.createRow(0);for(int i=0;i<columns.size();i++){header.createCell(i).setCellValue(columns.get(i));sheet.setColumnWidth(i,24*256);}sheet.createFreezePane(0,1);
   int number=1;for(var raw:rows){var data=display(raw);var row=sheet.createRow(number++);for(int i=0;i<columns.size();i++)row.createCell(i).setCellValue(Objects.toString(data.get(columns.get(i)),""));}
   var legend=workbook.createSheet("字段说明");String[] labels={"编号","卖家账户","卖家支付宝ID","平台流水号（原标签：报告编号）","商户订单号（原标签：样本编号）","交易金额（元）","交易状态","标题","内容","扩展","交易时间"};
   for(int i=0;i<columns.size();i++){var row=legend.createRow(i);row.createCell(0).setCellValue(columns.get(i));row.createCell(1).setCellValue(labels[i]);}legend.setColumnWidth(0,24*256);legend.setColumnWidth(1,50*256);
   workbook.write(output);return output.toByteArray();
  }catch(java.io.IOException e){throw new BusinessException(500,"流水导出生成失败，请重试");}
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

 public record BatchTarget(long id,String expectedToken){}
 public record BatchCommand(String operation,List<BatchTarget> targets,Map<String,String> fields,String requestKey){}
 public record BatchResult(String operation,List<Long> ids){}
 @Transactional public BatchResult batch(BatchCommand c){
  var p=policy(true);if(!p[4])throw new BusinessException(409,"交易流水批量维护尚未开启");
  if(c==null||!List.of("UPDATE","DELETE").contains(c.operation()==null?"":c.operation())||c.targets()==null||c.targets().isEmpty()||c.targets().size()>100)throw new BusinessException(400,"请选择1至100条流水及有效操作");
  boolean deleting=c.operation().equals("DELETE");if(deleting&&!p[2])throw new BusinessException(409,"交易流水删除尚未开启");request(c.requestKey());
  Map<String,String> patch=c.fields()==null?Map.of():c.fields();
  if(deleting&&!patch.isEmpty()||!deleting&&(patch.isEmpty()||!FIELDS.containsAll(patch.keySet())))throw new BusinessException(400,"批量修改字段无效");
  var targets=new TreeMap<Long,BatchTarget>();for(var t:c.targets()){if(t==null)throw new BusinessException(400,"流水快照缺失");id(t.id());version(t.expectedToken());if(targets.put(t.id(),t)!=null)throw new BusinessException(400,"不能重复选择同一条流水");}
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  Map<String,Object> canonical=Map.of("command",c);var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_PAYMENT_LOG_BATCH' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty()){try{var mapper=new ObjectMapper();var data=mapper.readTree(prior.get(0).get("after_json").toString());if(!mapper.readTree(store.encode(canonical)).equals(data.path("canonicalRequest")))throw new BusinessException(409,"请求号已用于其他批量操作");return mapper.treeToValue(data.path("result"),BatchResult.class);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"批量结果无法核对");}}
  var updates=new HashMap<Long,Map<String,String>>();for(var t:targets.values()){
   var before=raw(t.id(),true);if(!token(before).equals(t.expectedToken()))throw new BusinessException(409,"选中流水已变更，请重新勾选核对；本批次未写入");
   if(!deleting){var current=display(before);var fields=new TreeMap<String,String>();for(String f:FIELDS)fields.put(f,Objects.toString(current.get(f),""));fields.putAll(patch);updates.put(t.id(),normalized(fields));}
  }
  var ids=new ArrayList<Long>();for(var t:c.targets()){
   String key=token(Map.of("purpose","payment-log-batch-row-v1","actor",actor,"request",c.requestKey(),"operation",c.operation(),"id",t.id()));
   if(deleting)delete(t.id(),new DeleteCommand(key,t.expectedToken()));else save(t.id(),new Command(updates.get(t.id()),key,t.expectedToken()));ids.add(t.id());
  }
  var result=new BatchResult(c.operation(),List.copyOf(ids));store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,'LEGACY_PAYMENT_LOG_BATCHED','LEGACY_PAYMENT_LOG_BATCH','batch',?,?)",actor,c.requestKey(),store.encode(Map.of("canonicalRequest",canonical,"result",result)));return result;
 }
}
