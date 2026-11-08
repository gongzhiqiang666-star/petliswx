package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Existing User::SendPro1 drafts. Never runs administrative insertion/update callbacks. */
@Service @RequiredArgsConstructor
public class LegacyHospitalDraftService {
 private final FeatureStore store;
 private final AccessService access;
 private final LegacyHospitalHistoryService history;
 public record Command(Integer hospitalId,Map<String,String> fields,String expectedToken,String requestKey) {}
 private static final Set<String> FIELDS=Set.of("MasterName","AnimalName","AnimalTypeId","Age","DictSexId","CollectDt","InfoSampleTypeId","SampleArea","DoctorName","Remark");
 private static final Map<String,List<String>> DICTIONARIES=Map.of("variety",List.of("lin_lb_infovariety","IID","Variety_CN"),"sex",List.of("lin_lb_dictsex","DictSexId","SexName"),"sampleType",List.of("lin_lb_infosampletype","InfoSampleTypeId","SampleName"));
 void require(Integer hospital,boolean write){
  if(hospital==null||hospital<=0||!access.current().canAccessHospital(hospital))throw new BusinessException(403,"无权访问该医院草稿");
  if(!history.policy(write).get(write?"legacyHospitalDraftWrites":"legacyHospitalDrafts"))throw new BusinessException(409,write?"原医院草稿维护尚未开启":"原医院草稿查询尚未开启");
 }
 Map<String,Object> raw(long id,int hospital,boolean lock){
  if(id<1||id>Integer.MAX_VALUE)throw new BusinessException(400,"草稿编号无效");
  var guard=history.cancellationSnapshot(id,hospital,lock);
  if(((Number)guard.get("sampleStatus")).intValue()!=-2||LegacyHospitalHistoryService.hasNewWorkflow(guard))throw new BusinessException(409,"该单不是可编辑的原医院草稿");
  return store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? AND InfoHospitalId=?"+(lock?" FOR UPDATE":""),id,hospital);
 }
 @Transactional(readOnly=true) public Map<String,Object> list(int hospital,int page,int size){
  require(hospital,false);page(page,size);
  String where=" WHERE p.InfoHospitalId=? AND p.SampleStatus=-2 AND NOT EXISTS(SELECT 1 FROM lin_order_extension e WHERE e.order_id=p.ListDiagnosisProjectId) AND NOT EXISTS(SELECT 1 FROM lin_shipment s WHERE s.order_id=p.ListDiagnosisProjectId) AND NOT EXISTS(SELECT 1 FROM lin_report_workflow w WHERE w.order_id=p.ListDiagnosisProjectId)";
  return store.page("SELECT COUNT(*) FROM lin_lb_listdiagnosisproject p"+where,"SELECT p.ListDiagnosisProjectId AS orderId,p.AnimalName AS animalName,p.MasterName AS ownerName,p.DoctorName AS doctorName,DATE_FORMAT(p.CreateDt,'%Y-%m-%d %H:%i:%s') AS createdAt FROM lin_lb_listdiagnosisproject p"+where+" ORDER BY p.ListDiagnosisProjectId DESC",page,size,hospital);
 }
 @Transactional(readOnly=true) public Map<String,Object> detail(long id,int hospital){
  require(hospital,false);var row=raw(id,hospital,false);var fields=new TreeMap<String,Object>();for(String key:FIELDS)fields.put(key,row.get(key));
  if(row.get("CollectDt")!=null)fields.put("CollectDt",row.get("CollectDt").toString().substring(0,10));
  var labels=new LinkedHashMap<String,String>();
  for(var pair:Map.of("AnimalTypeId","variety","DictSexId","sex","InfoSampleTypeId","sampleType").entrySet()){
   Object selected=row.get(pair.getKey());var spec=DICTIONARIES.get(pair.getValue());String label="未选择";
   if(selected instanceof Number n&&n.longValue()!=0){var found=store.jdbc().queryForList("SELECT "+spec.get(2)+" AS label FROM "+spec.get(0)+" WHERE "+spec.get(1)+"=?",n.longValue());label=found.isEmpty()?"历史选项（未匹配）":LegacyDictionarySelector.display(found.get(0).get("label"));}labels.put(pair.getKey(),label);
  }
  return Map.of("orderId",id,"hospitalId",hospital,"fields",fields,"labels",labels,"editToken",history.cancellationToken(row),"writable",history.capabilities().get("legacyHospitalDraftWrites"));
 }
 @Transactional(readOnly=true) public Map<String,Object> choices(int hospital,String kind,String q,int page,int size){
  require(hospital,false);page(page,size);var spec=DICTIONARIES.get(kind);if(spec==null)throw new BusinessException(400,"草稿选项类型无效");
  q=q==null?"":q.trim();if(q.length()>100)throw new BusinessException(400,"选项搜索过长");String where=" WHERE "+spec.get(2)+" LIKE ?"+(kind.equals("sex")?" AND IsActive=0":"");
  var result=store.page("SELECT COUNT(*) FROM "+spec.get(0)+where,"SELECT CAST("+spec.get(1)+" AS CHAR) AS id,"+spec.get(2)+" AS name FROM "+spec.get(0)+where+" ORDER BY "+spec.get(1),page,size,"%"+q+"%");
  @SuppressWarnings("unchecked")var rows=(List<Map<String,Object>>)result.get("items");for(var row:rows)row.put("name",LegacyDictionarySelector.display(row.get("name")));return result;
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public long update(long id,Command command){
  if(command==null)throw new BusinessException(400,"草稿资料缺失");require(command.hospitalId(),true);
  if(command.fields()==null||command.fields().isEmpty()||!FIELDS.containsAll(command.fields().keySet()))throw new BusinessException(400,"草稿字段不允许修改");
  if(command.expectedToken()==null||!command.expectedToken().matches("[a-f0-9]{64}")||command.requestKey()==null||!command.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"请重新打开草稿后修改");
  var values=validate(command.fields());int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String canonical=store.encode(Map.of("orderId",id,"command",command));
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_DRAFT' AND request_id=? ORDER BY id DESC LIMIT 1",actor,command.requestKey());
  if(!prior.isEmpty())try{var saved=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(saved.path("canonicalRequest").asText()))throw new BusinessException(409,"草稿请求号已用于其他内容");return id;}catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new BusinessException(409,"草稿请求无法核对");}
  var before=raw(id,command.hospitalId(),true);if(!history.cancellationToken(before).equals(command.expectedToken()))throw new BusinessException(409,"草稿已被更新，请重新打开核对");
  var sets=new ArrayList<String>();var args=new ArrayList<Object>();values.forEach((key,value)->{sets.add("`"+key+"`=?");args.add(value);});args.add(id);args.add(command.hospitalId());
  if(store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET "+String.join(",",sets)+" WHERE ListDiagnosisProjectId=? AND InfoHospitalId=? AND SampleStatus=-2",args.toArray())!=1)throw new BusinessException(409,"草稿状态已变化，请重新打开");
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_HOSPITAL_DRAFT_UPDATED','LEGACY_HOSPITAL_DRAFT',?,?,?,?)",actor,String.valueOf(id),command.requestKey(),store.encode(before),store.encode(Map.of("canonicalRequest",canonical,"orderId",id)));return id;
 }
 private static Map<String,Object> validate(Map<String,String> fields){
  var result=new TreeMap<String,Object>();
  for(var entry:fields.entrySet()){
   String key=entry.getKey(),value=entry.getValue();if(value==null)throw new BusinessException(400,"草稿字段不能为空值");
   if(Set.of("AnimalTypeId","DictSexId","InfoSampleTypeId").contains(key)){try{int number=Integer.parseInt(value);if(number<0)throw new NumberFormatException();result.put(key,number);}catch(NumberFormatException error){throw new BusinessException(400,"草稿选项编号无效");}}
   else if(key.equals("CollectDt")){try{result.put(key,LocalDate.parse(value));}catch(DateTimeException error){throw new BusinessException(400,"采样日期无效；未修改时请保留原日期");}}
   else{int max=Set.of("MasterName","AnimalName").contains(key)?250:key.equals("DoctorName")?100:key.equals("Remark")?65535:50;if(value.codePointCount(0,value.length())>max||key.equals("Remark")&&value.getBytes(StandardCharsets.UTF_8).length>65535)throw new BusinessException(400,"草稿字段超过原长度");result.put(key,value);}
  }
  return result;
 }
 private static void page(int page,int size){if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw new BusinessException(400,"分页参数无效");}
}
