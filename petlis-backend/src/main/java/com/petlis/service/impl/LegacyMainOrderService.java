package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Service @RequiredArgsConstructor
public class LegacyMainOrderService {
 private final FeatureStore store;private final AccessService access;
 public record Command(Map<String,String> fields,String requestKey,String expectedToken){}
 private static final Set<String> ALLOWED=Set.of("Age","AnimalName","AnimalName_en","AnimalType","AnimalTypeId","AnimalType_en","Birthday","ChipNO","CityId","CollectDt","CreateDataTypeIId","DiagnosticResults","DictSexId","DoctorName","InfoAnimalId","InfoHospitalId","InfoSampleTypeId","MarketingName","MasterName","MasterName_en","ReceiveSampleDt","Remark","ReportNO","SampleArea","SampleStatus","SendReportDt","SendSampleDt","UserNO");
 private static final Set<String> INTEGERS=Set.of("AnimalTypeId","CityId","CreateDataTypeIId","DictSexId","InfoAnimalId","InfoHospitalId","InfoSampleTypeId","SampleStatus");
 private static final Set<String> DATES=Set.of("Birthday","CollectDt","ReceiveSampleDt","SendReportDt","SendSampleDt");
 private boolean[] policy(boolean lock){access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));if(rows.size()!=1)return new boolean[]{false,false};try{var j=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=j.path("enabled").isBoolean()&&j.path("enabled").asBoolean();return new boolean[]{read,read&&j.path("writeEnabled").isBoolean()&&j.path("writeEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"原主单参数无效");}}
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("mainOrders",p[0],"mainOrderWrites",p[1]);}
 private Map<String,Object> raw(long id,boolean lock){if(id<1||id>4294967295L)throw new BusinessException(400,"主单编号无效");return new LinkedHashMap<>(store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?"+(lock?" FOR UPDATE":""),id));}
 private String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @Transactional(readOnly=true) public Map<String,Object> detail(long id){if(!policy(false)[0])throw new BusinessException(409,"原送检主单尚未开启");var row=raw(id,false);row.put("editToken",token(row));return row;}
 @Transactional public long update(long id,Command c){
  if(!policy(true)[1])throw new BusinessException(409,"原送检主单维护尚未开启");
  if(c==null||c.fields()==null||c.fields().isEmpty()||c.fields().size()>ALLOWED.size()||!ALLOWED.containsAll(c.fields().keySet()))throw new BusinessException(400,"主单字段无效或不允许维护");
  if(c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}")||c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}"))throw new BusinessException(400,"请重新打开主单后修改");
  var values=LegacyMainOrderRules.update(new LinkedHashMap<>(c.fields()));
  for(var entry:values.entrySet()){
   String name=entry.getKey(),value=Objects.toString(entry.getValue(),"");
   if(INTEGERS.contains(name)){try{if(name.equals("CreateDataTypeIId"))Long.parseLong(value);else Integer.parseInt(value);}catch(NumberFormatException e){throw new BusinessException(400,"主单整数字段无效："+name);}}
   else if(DATES.contains(name)){try{if(value.length()==10)java.time.LocalDate.parse(value);else java.time.LocalDateTime.parse(value.replace(' ','T'));}catch(java.time.DateTimeException e){throw new BusinessException(400,"主单日期无效："+name);}}
   else{int max=Set.of("AnimalName","AnimalName_en","MasterName","MasterName_en").contains(name)?250:name.equals("DiagnosticResults")?255:Set.of("DoctorName","MarketingName").contains(name)?100:name.equals("Remark")?65535:50;if(value.codePointCount(0,value.length())>max||name.equals("Remark")&&value.getBytes(StandardCharsets.UTF_8).length>65535)throw new BusinessException(400,"主单字段超过原长度："+name);}
  }
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);String canonical=store.encode(Map.of("id",id,"command",c));
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_MAIN_ORDER' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty())try{var j=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(j.path("canonicalRequest").asText()))throw new BusinessException(409,"请求号已用于其他主单操作");return id;}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"主单请求无法核对");}
  var before=raw(id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"主单已变更，请重新打开核对");
  if(!values.isEmpty()){var args=new ArrayList<Object>();var sets=new ArrayList<String>();new TreeMap<>(values).forEach((key,value)->{sets.add("`"+key+"`=?");args.add(value);});args.add(id);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET "+String.join(",",sets)+" WHERE ListDiagnosisProjectId=?",args.toArray());}
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_MAIN_ORDER_UPDATED','LEGACY_MAIN_ORDER',?,?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(before),store.encode(Map.of("orderId",id,"canonicalRequest",canonical)));return id;
 }
}
