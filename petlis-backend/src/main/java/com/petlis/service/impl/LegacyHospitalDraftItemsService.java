package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;

/** User::SendPro2/infotesttype/addinfotesttype/delinfotesttype, scoped to existing hospital drafts. */
@Service @RequiredArgsConstructor
public class LegacyHospitalDraftItemsService {
 private final FeatureStore store;
 private final AccessService access;
 private final LegacyHospitalHistoryService history;
 private final LegacyHospitalDraftService drafts;
 public record Command(Integer hospitalId,Integer testId,String expectedToken,String requestKey) {}
 private record Snapshot(Map<String,Object> parent,List<Map<String,Object>> details){}
 private Snapshot snapshot(long order,int hospital,boolean lock){drafts.require(hospital,false);var parent=drafts.raw(order,hospital,lock);var details=store.jdbc().queryForList("SELECT * FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? ORDER BY DiagnosisDetailId"+(lock?" FOR UPDATE":""),order);return new Snapshot(parent,details);}
 private String token(Snapshot snapshot){return history.cancellationToken(Map.of("parent",snapshot.parent(),"details",snapshot.details()));}
 @Transactional(readOnly=true) public Map<String,Object> selected(long order,int hospital){
  var state=snapshot(order,hospital,false);
  var rows=store.jdbc().queryForList("SELECT DiagnosisDetailId AS detailId,InfoTestTypeId AS testId,TestName AS testName,TestNO AS testNo,CAST(TestPrice AS CHAR) AS price FROM vwdiagnosisdetail WHERE ListDiagnosisProjectId=? ORDER BY DiagnosisDetailId",order);
  return Map.of("items",rows,"editToken",token(state),"writable",history.capabilities().get("legacyHospitalDraftItems"));
 }
 @Transactional(readOnly=true) public Map<String,Object> catalog(long order,int hospital,String search,Integer category,int page,int size){
  drafts.require(hospital,false);drafts.raw(order,hospital,false);
  if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE)throw new BusinessException(400,"分页参数无效");
  search=search==null?"":search.trim();if(search.length()>100)throw new BusinessException(400,"项目搜索过长");
  var args=new ArrayList<Object>();args.add("%"+search+"%");String where=" WHERE t.IsActive=1 AND t.type<>0 AND t.TestName LIKE ?";
  if(category!=null){where+=" AND t.type=?";args.add(category);}
  long total=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_infotesttype t"+where,Long.class,args.toArray());
  var paged=new ArrayList<Object>();paged.add(order);paged.addAll(args);paged.add(size);paged.add((long)page*size);
  var rows=store.jdbc().queryForList("SELECT t.InfoTestTypeId AS testId,t.TestName AS testName,t.TestNO AS testNo,EXISTS(SELECT 1 FROM lin_lb_diagnosisdetail d WHERE d.ListDiagnosisProjectId=? AND d.InfoTestTypeId=t.InfoTestTypeId) AS selected FROM lin_lb_infotesttype t"+where+" ORDER BY t.InfoTestTypeId LIMIT ? OFFSET ?",paged.toArray());
  for(var row:rows){Object value=row.get("selected");row.put("selected",Boolean.TRUE.equals(value)||value instanceof Number n&&n.intValue()!=0);}
  return Map.of("items",rows,"total",total,"page",page,"size",size);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public long add(long order,Command command){return mutate(order,command,true);}
 @Transactional(isolation=Isolation.READ_COMMITTED) public long remove(long order,Command command){return mutate(order,command,false);}
 private long mutate(long order,Command command,boolean adding){
  if(command==null)throw new BusinessException(400,"请选择草稿与项目");drafts.require(command.hospitalId(),true);
  if(!history.policy(true).get("legacyHospitalDraftItems"))throw new BusinessException(409,"原草稿项目维护尚未开启");
  if(command.testId()==null||command.testId()<1||command.expectedToken()==null||!command.expectedToken().matches("[a-f0-9]{64}")||command.requestKey()==null||!command.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"请刷新已选项目后重试");
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String canonical=store.encode(Map.of("orderId",order,"command",command,"adding",adding));
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_DRAFT_ITEMS' AND request_id=? ORDER BY id DESC LIMIT 1",actor,command.requestKey());
  if(!prior.isEmpty())try{var saved=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(saved.path("canonicalRequest").asText()))throw new BusinessException(409,"项目请求号已用于其他内容");return saved.path("detailId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException error){throw new BusinessException(409,"项目请求无法核对");}
  var state=snapshot(order,command.hospitalId(),true);if(!token(state).equals(command.expectedToken()))throw new BusinessException(409,"草稿或检测项目已变化，请刷新后核对");
  var found=store.jdbc().queryForList("SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=? AND InfoTestTypeId=? LIMIT 1 FOR UPDATE",order,command.testId());long id;
  if(adding){
   if(!found.isEmpty())throw new BusinessException(409,"请勿重复添加");
   store.one("SELECT InfoTestTypeId FROM lin_lb_infotesttype WHERE InfoTestTypeId=?",command.testId());
   id=LegacyDetailModelWriter.insert(store,order,new LegacyOrderDetailService.CreateCommand(command.testId(),null,"10",null,"",0,"",0,0,0,0,"","",command.requestKey()));
  }else{
   if(found.isEmpty())throw new BusinessException(409,"原检测项目已不存在，请刷新");id=((Number)found.get(0).get("DiagnosisDetailId")).longValue();
   store.jdbc().update("DELETE FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=? AND ListDiagnosisProjectId=?",id,order);
   // No original afterDelete callback: do not erase body copies, attachments or history.
  }
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,?,'LEGACY_HOSPITAL_DRAFT_ITEMS',?,?,?)",actor,adding?"LEGACY_HOSPITAL_DRAFT_ITEM_ADDED":"LEGACY_HOSPITAL_DRAFT_ITEM_REMOVED",String.valueOf(order),command.requestKey(),store.encode(Map.of("canonicalRequest",canonical,"detailId",id,"orderId",order)));return id;
 }
}
