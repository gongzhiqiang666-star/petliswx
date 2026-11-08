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
/** PHP lb_infohospital edit business fields. No balance, credentials or historical time changes. */
@Service @RequiredArgsConstructor
public class LegacyHospitalProfileService {
 private final FeatureStore store;private final AccessService access;
 public record CreateCommand(Command profile,String loginName,String loginPassword,Integer active){}
 public record Command(String name,String englishName,String invoice,int cityId,String place,String address,String tel,String fax,int payType,String requestKey,String expectedToken){}
 private boolean[] policy(boolean lock){access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-hospital-profiles' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));if(rows.size()!=1)return new boolean[]{false,false,false};try{var json=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=json.path("enabled").isBoolean()&&json.path("enabled").asBoolean();boolean write=read&&json.path("writeEnabled").isBoolean()&&json.path("writeEnabled").asBoolean();return new boolean[]{read,write,write&&json.path("createEnabled").isBoolean()&&json.path("createEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"医院档案参数无效");}}
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("hospitalProfiles",p[0],"hospitalProfileWrites",p[1],"hospitalProfileCreates",p[2]);}
 private Map<String,Object> raw(long id,boolean lock){if(id<1||id>Integer.MAX_VALUE)throw new BusinessException(400,"医院编号无效");return new LinkedHashMap<>(store.one("SELECT InfoHospitalId,HospitalName,HospitalName_en,Invoice,CityId,Place,Adress,Tel,Fax,PayType FROM lin_lb_infohospital WHERE InfoHospitalId=?"+(lock?" FOR UPDATE":""),id));}
 private String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @Transactional(readOnly=true) public Map<String,Object> detail(long id){if(!policy(false)[0])throw new BusinessException(409,"医院档案迁移尚未开启");var row=raw(id,false);row.put("editToken",token(row));return row;}
 static void validate(Command c){validate(c,true);}
 private static void validate(Command c,boolean updating){
  if(c==null||c.name()==null||c.name().isBlank())throw new BusinessException(400,"医院名称不能为空");
  String[] texts={c.name(),c.englishName(),c.invoice(),c.tel(),c.fax()};int[] max={250,250,250,50,50};
  for(int i=0;i<texts.length;i++)if(texts[i]!=null&&texts[i].codePointCount(0,texts[i].length())>max[i])throw new BusinessException(400,"医院字段超过原长度");
  for(String text:new String[]{c.place(),c.address()})if(text!=null&&text.getBytes(StandardCharsets.UTF_8).length>65535)throw new BusinessException(400,"地址超过原字段长度");
  if(c.cityId()<0||c.payType()!=0&&c.payType()!=1)throw new BusinessException(400,"地区或结算方式无效");
  if(c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}")||updating&&(c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}")))throw new BusinessException(400,"请重新打开医院档案后编辑");
 }
 @Transactional public long update(long id,Command c){
  if(!policy(true)[1])throw new BusinessException(409,"医院档案维护尚未开启");validate(c);int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String canonical=store.encode(Map.of("hospitalId",id,"command",c));var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_PROFILE' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty())try{var json=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(json.path("canonicalRequest").asText()))throw new BusinessException(409,"请求号已用于其他医院操作");return id;}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"医院请求无法核对");}
  var before=raw(id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"医院档案已变更，请重新打开核对");
  store.jdbc().update("UPDATE lin_lb_infohospital SET HospitalName=?,HospitalName_en=?,Invoice=?,CityId=?,Place=?,Adress=?,Tel=?,Fax=?,PayType=? WHERE InfoHospitalId=?",c.name(),empty(c.englishName()),empty(c.invoice()),c.cityId(),empty(c.place()),empty(c.address()),empty(c.tel()),empty(c.fax()),c.payType(),id);
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_HOSPITAL_PROFILE_UPDATED','LEGACY_HOSPITAL_PROFILE',?,?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(before),store.encode(Map.of("hospitalId",id,"canonicalRequest",canonical)));return id;
 }
 @Transactional public long create(CreateCommand command){
  if(!policy(true)[2])throw new BusinessException(409,"医院新增尚未开启");
  if(command==null)throw new BusinessException(400,"医院资料缺失");var c=command.profile();validate(c,false);
  if(c.englishName()==null||c.englishName().isBlank())throw new BusinessException(400,"医院英文名不能为空");
  int active=command.active()==null?0:command.active();if(active!=0&&active!=1)throw new BusinessException(400,"医院状态无效");
  for(String value:new String[]{command.loginName(),command.loginPassword()})if(value!=null&&value.codePointCount(0,value.length())>50)throw new BusinessException(400,"原医院登录字段不能超过50字符");
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  // Fingerprint includes supplied legacy credentials, but the audit never stores those plaintext inputs.
  String fingerprint=token(Map.of("operation","CREATE","command",command));
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_PROFILE' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty())try{var json=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!fingerprint.equals(json.path("requestFingerprint").asText())||!json.path("hospitalId").canConvertToLong())throw new BusinessException(409,"请求号已用于其他医院操作");return json.path("hospitalId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"医院请求无法核对");}
  long id=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,Invoice,CityId,Place,Adress,Tel,Fax,PayType,IsActive,LoginName,LoginPwd) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",c.name(),c.englishName(),empty(c.invoice()),c.cityId(),empty(c.place()),empty(c.address()),empty(c.tel()),empty(c.fax()),c.payType(),active,empty(command.loginName()),empty(command.loginPassword()));
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,'LEGACY_HOSPITAL_PROFILE_CREATED','LEGACY_HOSPITAL_PROFILE',?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(Map.of("hospitalId",id,"requestFingerprint",fingerprint,"profile",raw(id,false))));return id;
 }
 private static String empty(String value){return value==null?"":value;}
}
