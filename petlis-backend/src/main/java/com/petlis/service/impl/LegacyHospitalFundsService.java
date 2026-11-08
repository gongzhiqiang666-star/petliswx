package com.petlis.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.util.*;

/** Hospital pool only. Old admin IDs and new account IDs are distinct identities. No ledger update/delete API. */
@Service @RequiredArgsConstructor
public class LegacyHospitalFundsService {
 private final FeatureStore store;private final AccessService access;
 private static final BigDecimal MAX=new BigDecimal("99999999.99");
 public record Command(BigDecimal amount,String remark,String requestKey){}
 private record Policy(boolean enabled,boolean writable,JsonNode mappings){}
 private Policy policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-hospital-funds' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR SHARE":""));
  if(rows.size()!=1)return new Policy(false,false,null);
  try{var json=new ObjectMapper().readTree(String.valueOf(rows.get(0).get("config_value")));boolean enabled=json.path("enabled").isBoolean()&&json.path("enabled").asBoolean();return new Policy(enabled,enabled&&json.path("writeEnabled").isBoolean()&&json.path("writeEnabled").asBoolean(),json.path("adminMappings"));}
  catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"医院资金配置无效");}
 }
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("hospitalFunds",p.enabled(),"hospitalFundWrites",p.writable());}
 private void requireRead(){if(!policy(false).enabled())throw new BusinessException(409,"医院资金迁移模块尚未开启");}
 private Map<String,Object> hospital(long id,boolean lock){
  if(id<1||id>4294967295L)throw new BusinessException(400,"医院编号无效");
  var rows=store.jdbc().queryForList("SELECT InfoHospitalId,HospitalName,amount FROM lin_lb_infohospital WHERE InfoHospitalId=?"+(lock?" FOR UPDATE":""),id);
  if(rows.isEmpty())throw new BusinessException(404,"医院不存在");return rows.get(0);
 }
 @Transactional(readOnly=true) public Map<String,Object> summary(long hospitalId){requireRead();var r=new LinkedHashMap<>(hospital(hospitalId,false));r.put("fundPool","HOSPITAL");return r;}
 @Transactional(readOnly=true) public Map<String,Object> entries(long hospitalId,int page,int size){
  requireRead();hospital(hospitalId,false);if(page<0||size<1||size>100)throw new BusinessException(400,"分页无效");
  return store.page("SELECT COUNT(*) FROM lin_infohospital_money WHERE infohospital_id=?","SELECT m.id,m.infohospital_id,h.HospitalName,m.amount,m.admin_id,m.remark,m.createtime,m.updatetime FROM lin_infohospital_money m LEFT JOIN lin_lb_infohospital h ON h.InfoHospitalId=m.infohospital_id WHERE m.infohospital_id=? ORDER BY m.id DESC",page,size,hospitalId);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public long adjust(long hospitalId,Command command){
  var p=policy(true);if(!p.writable())throw new BusinessException(409,"医院资金维护尚未开启");
  if(command==null||command.amount()==null||command.amount().signum()==0)throw new BusinessException(400,"金额不允许为空");
  if(command.amount().stripTrailingZeros().scale()>2||command.amount().abs().compareTo(MAX)>0)throw new BusinessException(400,"金额最多两位小数，且不能超出原账户范围");
  if(command.remark()==null||command.remark().isBlank()||command.remark().codePointCount(0,command.remark().length())>255)throw new BusinessException(400,"请填写255字以内的备注");
  if(command.requestKey()==null||!command.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"资金请求号无效");
  if(hospitalId<1||hospitalId>4294967295L)throw new BusinessException(400,"医院编号无效");
  int actor=access.current().userId();
  store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  var payload=Map.of("hospitalId",hospitalId,"amount",command.amount().setScale(2).toPlainString(),"remark",command.remark());String canonical=store.encode(new TreeMap<>(payload));
  var old=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_FUNDS' AND request_id=? ORDER BY id DESC LIMIT 1",actor,command.requestKey());
  if(!old.isEmpty()){
   try{var record=new ObjectMapper().readTree(String.valueOf(old.get(0).get("after_json")));if(!canonical.equals(record.path("canonicalRequest").asText())||!record.path("ledgerId").canConvertToLong())throw new BusinessException(409,"请求号已用于其他资金操作");return record.path("ledgerId").asLong();}
   catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"原资金请求无法校验");}
  }
  var mapped=p.mappings()==null?null:p.mappings().path(String.valueOf(actor));
  if(mapped==null||!mapped.isIntegralNumber()||!mapped.canConvertToInt()||mapped.asInt()<1)throw new BusinessException(409,"请先配置此账号对应的原管理员身份");
  int legacyAdmin=mapped.asInt();var admins=store.jdbc().queryForList("SELECT id FROM lin_admin WHERE id=? AND status='normal' FOR SHARE",legacyAdmin);
  if(admins.size()!=1)throw new BusinessException(409,"原管理员身份不存在或已停用");
  var h=hospital(hospitalId,true);BigDecimal before=h.get("amount")==null?null:new BigDecimal(h.get("amount").toString());
  BigDecimal after=LegacyHospitalFundsRules.balanceAfter(before,command.amount(),String.valueOf(h.get("HospitalName")));
  if(after.abs().compareTo(MAX)>0)throw new BusinessException(400,"余额超出原账户范围");
  store.jdbc().update("UPDATE lin_lb_infohospital SET amount=? WHERE InfoHospitalId=?",after,hospitalId);
  long ledger=store.insert("INSERT INTO lin_infohospital_money(infohospital_id,amount,remark,admin_id,createtime,updatetime) VALUES(?,?,?,?,UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",hospitalId,command.amount(),command.remark(),legacyAdmin);
  var audit=new LinkedHashMap<String,Object>();audit.put("canonicalRequest",canonical);audit.put("ledgerId",ledger);audit.put("balance",after);audit.put("legacyAdminId",legacyAdmin);audit.put("fundPool","HOSPITAL");
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'HOSPITAL_FUNDS_ADJUSTED','LEGACY_HOSPITAL_FUNDS',?,?,?,?)",actor,String.valueOf(hospitalId),command.requestKey(),store.encode(Map.of("balance",before,"fundPool","HOSPITAL")),store.encode(audit));
  return ledger;
 }
}
