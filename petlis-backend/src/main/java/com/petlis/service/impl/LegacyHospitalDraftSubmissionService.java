package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;

/** User::SendPro3: source preview plus draft -2 -> pending 0; no implicit workflow adoption. */
@Service @RequiredArgsConstructor
public class LegacyHospitalDraftSubmissionService {
 private final FeatureStore store;
 private final AccessService access;
 private final LegacyHospitalHistoryService history;
 private final LegacyHospitalDraftService drafts;
 private final LegacyHospitalDraftItemsService items;
 public record Command(Integer hospitalId,String expectedToken,String requestKey){}
 @Transactional(readOnly=true) public Map<String,Object> preview(long order,int hospital){
  var state=items.snapshot(order,hospital,false);var presentation=presentation(state);
  var fields=new LinkedHashMap<String,Object>();for(String key:List.of("MasterName","AnimalName","Age","DoctorName","CollectDt","SampleArea","Remark"))fields.put(key,Objects.toString(state.parent().get(key),""));
  String collected=Objects.toString(fields.get("CollectDt"),"");if(collected.length()>=10)fields.put("CollectDt",collected.startsWith("1900-01-01")?"":collected.substring(0,10));
  var selected=items.selected(order,hospital);var result=new LinkedHashMap<String,Object>();result.putAll(presentation);result.put("fields",fields);result.put("items",selected.get("items"));result.put("orderId",order);result.put("hospitalId",hospital);result.put("editToken",token(state,presentation));result.put("writable",history.capabilities().get("legacyHospitalDraftSubmit"));return result;
 }
 private Map<String,String> presentation(LegacyHospitalDraftItemsService.Snapshot state){
  long variety=number(state.parent().get("AnimalTypeId")),sex=number(state.parent().get("DictSexId"));String current="",legacy="",gender="";
  // PHP array_merge([0=>""], column(...,"IID")) reindexes numeric keys. Do not silently fix it.
  var varieties=store.jdbc().queryForList("SELECT Variety_CN,IID FROM lin_lb_infovariety");
  for(int i=0;i<varieties.size();i++){var row=varieties.get(i);String label=LegacyDictionarySelector.display(row.get("Variety_CN"));if(number(row.get("IID"))==variety)current=label;if(i+1==variety)legacy=label;}
  var genders=store.jdbc().queryForList("SELECT SexName FROM lin_lb_dictsex WHERE DictSexId=? AND IsActive=0",sex);if(!genders.isEmpty())gender=LegacyDictionarySelector.display(genders.get(0).get("SexName"));
  var configs=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-hospital-history' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");String mode="PENDING";
  if(configs.size()==1)try{String configured=new ObjectMapper().readTree(configs.get(0).get("config_value").toString()).path("previewVarietyMode").asText();if(Set.of("ID","LEGACY").contains(configured))mode=configured;}catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new BusinessException(409,"预览参数无效");}
  return Map.of("idVarietyName",current,"legacyVarietyName",legacy,"varietyName",mode.equals("ID")?current:mode.equals("LEGACY")?legacy:"","sexName",gender,"previewMode",mode);
 }
 private String token(LegacyHospitalDraftItemsService.Snapshot state,Map<String,String> presentation){return history.cancellationToken(Map.of("record",items.token(state),"presentation",new TreeMap<>(presentation)));}
 private static long number(Object value){return value instanceof Number n?n.longValue():0;}
 @Transactional(isolation=Isolation.READ_COMMITTED) public Map<String,Object> submit(long order,Command command){
  if(command==null)throw new BusinessException(400,"提交资料缺失");drafts.require(command.hospitalId(),true);
  if(!history.policy(true).get("legacyHospitalDraftSubmit"))throw new BusinessException(409,"原草稿提交尚未开启或品种预览口径待确认");
  if(command.expectedToken()==null||!command.expectedToken().matches("[a-f0-9]{64}")||command.requestKey()==null||!command.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"请重新预览后提交");
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);String canonical=store.encode(Map.of("orderId",order,"command",command));
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_DRAFT_SUBMIT' AND request_id=? ORDER BY id DESC LIMIT 1",actor,command.requestKey());
  if(!prior.isEmpty())try{var saved=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!LegacyCanonicalRequest.same(canonical,saved.path("canonicalRequest").asText()))throw new BusinessException(409,"提交请求号已用于不同内容");return Map.of("orderId",order,"sampleStatus",0);}catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new BusinessException(409,"提交请求记录无法核对");}
  var state=items.snapshot(order,command.hospitalId(),true);if(!token(state,presentation(state)).equals(command.expectedToken()))throw new BusinessException(409,"资料、检测项目或预览内容已变化，请重新预览");
  if(store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=0 WHERE ListDiagnosisProjectId=? AND InfoHospitalId=? AND SampleStatus=-2",order,command.hospitalId())!=1)throw new BusinessException(409,"草稿已提交或状态变化");
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_HOSPITAL_DRAFT_SUBMITTED','LEGACY_HOSPITAL_DRAFT_SUBMIT',?,?,?,?)",actor,String.valueOf(order),command.requestKey(),store.encode(state.parent()),store.encode(Map.of("canonicalRequest",canonical,"sampleStatus",0)));
  return Map.of("orderId",order,"sampleStatus",0);
 }
}
