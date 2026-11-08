package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(isolation=Isolation.READ_COMMITTED)
public class HospitalSubmissionService {
 private final FeatureStore store;
 private final AccessService access;
 private final PlatformFileService files;
 private final BusinessNotifications notifications;
 private static final String SUBMITTED="HOSPITAL_DOCUMENT_SUBMITTED",REVIEWED="HOSPITAL_DOCUMENT_REVIEWED";
 private static final String JOINS=" FROM lin_file f LEFT JOIN lin_audit_record s ON s.resource_type='HOSPITAL_DOCUMENT' AND s.action='HOSPITAL_DOCUMENT_SUBMITTED' AND CAST(s.resource_id AS UNSIGNED)=f.id LEFT JOIN lin_audit_record u ON u.resource_type='HOSPITAL_DOCUMENT' AND u.action='HOSPITAL_DOCUMENT_UPLOADED' AND CAST(u.resource_id AS UNSIGNED)=f.id LEFT JOIN lin_audit_record r ON r.resource_type='HOSPITAL_DOCUMENT' AND r.action='HOSPITAL_DOCUMENT_REVIEWED' AND CAST(r.resource_id AS UNSIGNED)=f.id ";
 private static final String COLUMNS="SELECT f.id,f.hospital_id AS hospitalId,f.original_name AS originalName,f.file_size AS fileSize,f.mime_type AS mimeType,f.created_at AS createdAt,COALESCE(JSON_UNQUOTE(JSON_EXTRACT(s.after_json,'$.purpose')),JSON_UNQUOTE(JSON_EXTRACT(u.after_json,'$.purpose'))) AS purpose,COALESCE(JSON_UNQUOTE(JSON_EXTRACT(s.after_json,'$.reason')),JSON_UNQUOTE(JSON_EXTRACT(u.after_json,'$.reason'))) AS reason,COALESCE(JSON_UNQUOTE(JSON_EXTRACT(s.after_json,'$.previousFileId')),JSON_UNQUOTE(JSON_EXTRACT(u.after_json,'$.previousFileId'))) AS previousFileId,CASE WHEN f.biz_type='HOSPITAL_DOCUMENT' THEN 'APPROVED' ELSE COALESCE(JSON_UNQUOTE(JSON_EXTRACT(r.after_json,'$.status')),'PENDING') END AS reviewStatus,JSON_UNQUOTE(JSON_EXTRACT(r.after_json,'$.reason')) AS reviewReason,EXISTS(SELECT 1 FROM lin_audit_record n WHERE n.resource_type='HOSPITAL_DOCUMENT' AND n.action='HOSPITAL_DOCUMENT_UPLOADED' AND CAST(JSON_UNQUOTE(JSON_EXTRACT(n.after_json,'$.previousFileId')) AS UNSIGNED)=f.id) AS replaced";
 public Map<String,Object> list(int hospital,int page,int size){
  access.requireHospitalAdmin(hospital);hospital(hospital);
  String where=" WHERE f.hospital_id=? AND f.biz_type IN ('HOSPITAL_DOCUMENT','HOSPITAL_DOCUMENT_REVIEW') AND f.deleted_at IS NULL";
  return store.page("SELECT COUNT(*)"+JOINS+where,COLUMNS+JOINS+where+" ORDER BY f.id DESC",page,size,hospital);
 }
 public List<Map<String,Object>> hospitals(){
  var actor=access.current();
  if(!actor.isAdmin() && !actor.hasRole(com.petlis.common.security.PlatformRole.HOSPITAL_ADMIN))throw new BusinessException(403,"需要医院管理员权限");
  if(actor.isAdmin())return store.jdbc().queryForList("SELECT InfoHospitalId AS id,HospitalName AS name FROM lin_lb_infohospital ORDER BY InfoHospitalId DESC");
  var result=new ArrayList<Map<String,Object>>();for(int id:new TreeSet<>(access.managedHospitalIds()))result.add(store.one("SELECT InfoHospitalId AS id,HospitalName AS name FROM lin_lb_infohospital WHERE InfoHospitalId=?",id));return result;
 }
 public Map<String,Object> pending(int page,int size){
  access.requireAdmin();String where=" WHERE f.biz_type='HOSPITAL_DOCUMENT_REVIEW' AND f.deleted_at IS NULL AND r.id IS NULL";
  return store.page("SELECT COUNT(*)"+JOINS+where,COLUMNS+",h.HospitalName AS hospitalName"+JOINS+" JOIN lin_lb_infohospital h ON h.InfoHospitalId=f.hospital_id"+where+" ORDER BY f.id",page,size);
 }
 public Map<String,Object> submit(int hospital,MultipartFile file,String purpose,String reason,String key,Long previous){
  access.requireHospitalAdmin(hospital);validate(purpose,reason,key,previous);
  if(file==null || file.isEmpty() || file.getSize()>10*1024*1024)throw new BusinessException(400,"资料文件不能为空且需小于10MB");
  String digest;try{digest=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(file.getBytes()));}catch(Exception e){throw new BusinessException(400,"资料文件无法读取");}
  String payload=store.encode(Arrays.asList(hospital,purpose,reason.trim(),previous,file.getOriginalFilename(),file.getContentType(),digest));
  int actor=access.current().userId();lockActor(actor);hospital(hospital);
  var replay=replay(actor,SUBMITTED,key,payload);if(replay!=null)return replay;
  previous(hospital,purpose,previous,true);
  long id=files.saveHospitalSubmission(hospital,file);var data=new LinkedHashMap<String,Object>();data.put("purpose",purpose);data.put("reason",reason.trim());data.put("previousFileId",previous);data.put("requestPayload",payload);data.put("status","PENDING");audit(actor,SUBMITTED,id,key,data);
  return view(id);
 }
 public record Decision(boolean approved,String reason,String requestKey){}
 public Map<String,Object> review(long id,Decision decision){
  access.requireAdmin();if(decision==null || decision.reason()==null || decision.reason().isBlank() || decision.reason().length()>500 || decision.requestKey()==null || !decision.requestKey().matches("[A-Za-z0-9_-]{16,128}"))throw new BusinessException(400,"请填写审核意见和有效请求编号");
  int actor=access.current().userId();lockActor(actor);String payload=store.encode(List.of(id,decision.approved(),decision.reason().trim()));
  var replay=replay(actor,REVIEWED,decision.requestKey(),payload);if(replay!=null)return replay;
  var probe=store.one("SELECT hospital_id FROM lin_file WHERE id=? AND biz_type IN ('HOSPITAL_DOCUMENT_REVIEW','HOSPITAL_DOCUMENT') AND deleted_at IS NULL",id);int hospital=((Number)probe.get("hospital_id")).intValue();hospital(hospital);
  store.one("SELECT id FROM lin_file WHERE id=? AND biz_type IN ('HOSPITAL_DOCUMENT_REVIEW','HOSPITAL_DOCUMENT') AND deleted_at IS NULL FOR UPDATE",id);
  if(!store.jdbc().queryForList("SELECT id FROM lin_audit_record WHERE action=? AND resource_type='HOSPITAL_DOCUMENT' AND resource_id=? FOR UPDATE",REVIEWED,String.valueOf(id)).isEmpty())throw new BusinessException(409,"资料申请已审核，请刷新");
  var submission=store.one("SELECT actor_user_id,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.purpose')) AS purpose,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.reason')) AS reason,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.previousFileId')) AS previousFileId FROM lin_audit_record WHERE action=? AND resource_type='HOSPITAL_DOCUMENT' AND resource_id=?",SUBMITTED,String.valueOf(id));
  if(((Number)submission.get("actor_user_id")).intValue()==actor)throw new BusinessException(403,"不能审核本人提交的医院资料");
  var data=submission;String purpose=Objects.toString(data.get("purpose"));Object prior=data.get("previousFileId");Long previous=prior==null || "null".equals(prior.toString())?null:Long.valueOf(prior.toString());
  if(decision.approved()){
   previous(hospital,purpose,previous,false);
   store.jdbc().update("UPDATE lin_file SET biz_type='HOSPITAL_DOCUMENT' WHERE id=?",id);
   var archive=new LinkedHashMap<String,Object>();archive.put("purpose",purpose);archive.put("reason",data.get("reason"));archive.put("previousFileId",previous);archive.put("submissionId",id);audit(actor,"HOSPITAL_DOCUMENT_UPLOADED",id,null,archive);
  }
  audit(actor,REVIEWED,id,decision.requestKey(),Map.of("status",decision.approved()?"APPROVED":"REJECTED","reason",decision.reason().trim(),"requestPayload",payload));
  notifications.emit(((Number)submission.get("actor_user_id")).intValue(),"profile","hospital-document:"+id,"医院资料审核结果",(decision.approved()?"医院资料已通过审核：":"医院资料未通过审核：")+decision.reason().trim(),"pages/mine/mine");return view(id);
 }
 private void previous(int hospital,String purpose,Long previous,boolean blockPending){
  if(previous==null)return;
  store.one("SELECT id FROM lin_file WHERE id=? AND hospital_id=? AND biz_type='HOSPITAL_DOCUMENT' AND deleted_at IS NULL FOR UPDATE",previous,hospital);
  var row=store.one("SELECT JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.purpose')) AS purpose FROM lin_audit_record WHERE resource_type='HOSPITAL_DOCUMENT' AND action='HOSPITAL_DOCUMENT_UPLOADED' AND resource_id=?",previous.toString());
  if(!purpose.equals(row.get("purpose")))throw new BusinessException(409,"替换文件资料类型不一致");
  if(!store.jdbc().queryForList("SELECT id FROM lin_audit_record WHERE resource_type='HOSPITAL_DOCUMENT' AND action='HOSPITAL_DOCUMENT_UPLOADED' AND JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.previousFileId'))=? FOR UPDATE",previous.toString()).isEmpty())throw new BusinessException(409,"原文件已被替换，请刷新核对当前版本");
  if(blockPending && !store.jdbc().queryForList("SELECT s.id FROM lin_audit_record s WHERE s.resource_type='HOSPITAL_DOCUMENT' AND s.action=? AND JSON_UNQUOTE(JSON_EXTRACT(s.after_json,'$.previousFileId'))=? AND NOT EXISTS(SELECT 1 FROM lin_audit_record r WHERE r.resource_type=s.resource_type AND r.resource_id=s.resource_id AND r.action=?) FOR UPDATE",SUBMITTED,previous.toString(),REVIEWED).isEmpty())throw new BusinessException(409,"该版本已有待审核替换申请");
 }
 private Map<String,Object> replay(int actor,String action,String key,String payload){
  var rows=store.jdbc().queryForList("SELECT resource_id,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.requestPayload')) AS payload FROM lin_audit_record WHERE actor_user_id=? AND resource_type='HOSPITAL_DOCUMENT' AND action=? AND request_id=? FOR UPDATE",actor,action,key);
  if(rows.isEmpty())return null;if(rows.size()!=1 || !payload.equals(rows.get(0).get("payload")))throw new BusinessException(409,"原请求内容不一致，请刷新核对结果");return view(Long.parseLong(rows.get(0).get("resource_id").toString()));
 }
 private void audit(int actor,String action,long id,String key,Map<String,Object> data){store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,?,'HOSPITAL_DOCUMENT',?,?,?)",actor,action,Long.toString(id),key,store.encode(data));}
 private Map<String,Object> view(long id){return Map.of("id",id,"url","/platform-files/"+id+"/download");}
 private void hospital(int hospital){if(hospital<=0)throw new BusinessException(400,"医院编号无效");store.one("SELECT InfoHospitalId FROM lin_lb_infohospital WHERE InfoHospitalId=? FOR UPDATE",hospital);}
 private void lockActor(int actor){store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);}
 private void validate(String purpose,String reason,String key,Long previous){if(purpose==null || !Set.of("LICENSE","COOPERATION","OTHER").contains(purpose) || reason==null || reason.isBlank() || reason.length()>500 || key==null || !key.matches("[A-Za-z0-9_-]{16,128}") || previous!=null && previous<=0)throw new BusinessException(400,"请填写有效资料类型、原因和请求编号");}
}
