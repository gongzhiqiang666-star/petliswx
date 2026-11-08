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
import java.time.*;
@Service @RequiredArgsConstructor
public class LegacyHospitalContactService {
 private final FeatureStore store;private final AccessService access;
 public record DeleteCommand(String requestKey,String expectedToken){}
 public record Command(String name,String tel,String mail,String fax,String remark,int financeContact,String requestKey,String expectedToken){}
 private boolean[] policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-hospital-contacts' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));
  if(rows.size()!=1)return new boolean[]{false,false,false};
  try{var json=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean enabled=json.path("enabled").isBoolean()&&json.path("enabled").asBoolean();boolean write=enabled&&json.path("writeEnabled").isBoolean()&&json.path("writeEnabled").asBoolean();return new boolean[]{enabled,write,write&&json.path("deleteEnabled").isBoolean()&&json.path("deleteEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"联系人维护参数无效");}
 }
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("hospitalContacts",p[0],"hospitalContactWrites",p[1],"hospitalContactDeletes",p[2]);}
 private void read(long hospital){if(!policy(false)[0])throw new BusinessException(409,"医院联系人模块尚未开启");hospital(hospital);}
 private void hospital(long hospital){if(hospital<1||hospital>Integer.MAX_VALUE)throw new BusinessException(400,"医院编号无效");store.one("SELECT InfoHospitalId FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital);}
 private Map<String,Object> raw(long hospital,long id,boolean lock){return new LinkedHashMap<>(store.one("SELECT InfoHospitalContactId,InfoHospitalId,isFinanceContact,Name,Tel,Mail,Fax,ContactRemark,CreateDt FROM lin_lb_infohospitalcontact WHERE InfoHospitalId=? AND InfoHospitalContactId=?"+(lock?" FOR UPDATE":""),hospital,id));}
 private String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @Transactional(readOnly=true) public Map<String,Object> list(long hospital,int page,int size){read(hospital);if(page<0||size<1||size>100)throw new BusinessException(400,"分页无效");return store.page("SELECT COUNT(*) FROM lin_lb_infohospitalcontact WHERE InfoHospitalId=?","SELECT InfoHospitalContactId,InfoHospitalId,isFinanceContact,Name,Tel,Mail,Fax,ContactRemark,CreateDt FROM lin_lb_infohospitalcontact WHERE InfoHospitalId=? ORDER BY InfoHospitalContactId",page,size,hospital);}
 @Transactional(readOnly=true) public Map<String,Object> detail(long hospital,long id){read(hospital);var row=raw(hospital,id,false);row.put("editToken",token(row));return row;}
 /** Original report/month-report selectors include all hospital contacts, not only finance=1. */
 @Transactional(readOnly=true) public Map<String,Object> recipient(long hospital,long id){
  read(hospital);var row=raw(hospital,id,false);String mail=Objects.toString(row.get("Mail"),"").trim();
  if(mail.isEmpty())throw new BusinessException(409,"联系人邮箱为空，请先维护");
  return Map.of("contactId",id,"hospitalId",hospital,"name",Objects.toString(row.get("Name"),""),"mail",mail,"financeContact",row.get("isFinanceContact"));
 }
 @Transactional public long save(long hospital,Long id,Command c){
  if(!policy(true)[1])throw new BusinessException(409,"医院联系人维护尚未开启");hospital(hospital);
  if(c==null||c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"联系人请求号无效");
  var value=LegacyHospitalContactRules.validate(new LegacyHospitalContactRules.Contact(c.name(),c.tel(),c.mail(),c.fax(),c.remark(),c.financeContact()));
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  var payload=new TreeMap<String,Object>();payload.put("hospital",hospital);payload.put("id",id);payload.put("command",c);String canonical=store.encode(payload);
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_CONTACT' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty())try{var json=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(json.path("canonicalRequest").asText()))throw new BusinessException(409,"请求号已用于其他联系人操作");return json.path("contactId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"无法核对联系人请求");}
  Map<String,Object> before=null;long result;
  if(id==null){result=store.insert("INSERT INTO lin_lb_infohospitalcontact(InfoHospitalId,isFinanceContact,Name,Tel,Mail,Fax,ContactRemark,CreateDt) VALUES(?,?,?,?,?,?,?,?)",hospital,value.financeContact(),value.name(),value.tel(),value.mail(),value.fax(),value.remark(),LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0));}
  else{before=raw(hospital,id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"联系人已变更，请重新打开核对");store.jdbc().update("UPDATE lin_lb_infohospitalcontact SET isFinanceContact=?,Name=?,Tel=?,Mail=?,Fax=?,ContactRemark=? WHERE InfoHospitalContactId=? AND InfoHospitalId=?",value.financeContact(),value.name(),value.tel(),value.mail(),value.fax(),value.remark(),id,hospital);result=id;}
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_HOSPITAL_CONTACT_SAVED','LEGACY_HOSPITAL_CONTACT',?,?,?,?)",actor,String.valueOf(result),c.requestKey(),before==null?null:store.encode(before),store.encode(Map.of("contactId",result,"canonicalRequest",canonical)));return result;
 }
 @Transactional public long delete(long hospital,long id,DeleteCommand c){
  if(!policy(true)[2])throw new BusinessException(409,"医院联系人删除尚未开启");hospital(hospital);
  if(c==null||c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}")||c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}"))throw new BusinessException(400,"请重新打开联系人后删除");
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String canonical=store.encode(Map.of("operation","DELETE","hospital",hospital,"id",id,"command",c));
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_CONTACT' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty())try{var value=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(value.path("canonicalRequest").asText()))throw new BusinessException(409,"请求号已用于其他联系人操作");return id;}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"无法核对删除请求");}
  var before=raw(hospital,id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"联系人已变更，请重新打开核对");
  store.jdbc().update("DELETE FROM lin_lb_infohospitalcontact WHERE InfoHospitalContactId=? AND InfoHospitalId=?",id,hospital);
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_HOSPITAL_CONTACT_DELETED','LEGACY_HOSPITAL_CONTACT',?,?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(before),store.encode(Map.of("contactId",id,"canonicalRequest",canonical)));return id;
 }

}
