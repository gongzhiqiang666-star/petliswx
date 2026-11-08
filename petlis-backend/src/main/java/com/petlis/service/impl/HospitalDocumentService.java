package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@Service @RequiredArgsConstructor
@Transactional(isolation=Isolation.READ_COMMITTED)
public class HospitalDocumentService {
 private final FeatureStore store;
 private final AccessService access;
 private final PlatformFileService files;
 public Map<String,Object> list(int hospital,int page,int size){
  access.requireAdmin();hospital(hospital,false);
  String from=" FROM lin_file f LEFT JOIN lin_audit_record a ON a.resource_type='HOSPITAL_DOCUMENT' AND a.action='HOSPITAL_DOCUMENT_UPLOADED' AND CAST(a.resource_id AS UNSIGNED)=f.id WHERE f.biz_type='HOSPITAL_DOCUMENT' AND f.hospital_id=? AND f.deleted_at IS NULL";
  return store.page("SELECT COUNT(*)"+from,"SELECT f.id,f.original_name AS originalName,f.mime_type AS mimeType,f.file_size AS fileSize,f.created_at AS createdAt,JSON_UNQUOTE(JSON_EXTRACT(a.after_json,'$.purpose')) AS purpose,JSON_UNQUOTE(JSON_EXTRACT(a.after_json,'$.reason')) AS reason,JSON_UNQUOTE(JSON_EXTRACT(a.after_json,'$.previousFileId')) AS previousFileId,EXISTS(SELECT 1 FROM lin_audit_record next_version WHERE next_version.action='HOSPITAL_DOCUMENT_UPLOADED' AND next_version.resource_type='HOSPITAL_DOCUMENT' AND CAST(JSON_UNQUOTE(JSON_EXTRACT(next_version.after_json,'$.previousFileId')) AS UNSIGNED)=f.id) AS replaced"+from+" ORDER BY f.id DESC",page,size,hospital);
 }
 public Map<String,Object> upload(int hospital,MultipartFile file,String purpose,String reason,String key,Long previous){
  access.requireAdmin();if(purpose==null || !Set.of("LICENSE","COOPERATION","OTHER").contains(purpose) || reason==null || reason.isBlank() || reason.length()>500 || key==null || !key.matches("[A-Za-z0-9_-]{16,128}") || previous!=null && previous<=0)throw new BusinessException(400,"请填写有效资料类型、原因和上传请求编号");
  if(file==null || file.isEmpty() || file.getSize()>10*1024*1024)throw new BusinessException(400,"资料文件不能为空且需小于10MB");
  String fingerprint;try{fingerprint=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(file.getBytes()));}catch(Exception e){throw new BusinessException(400,"资料文件无法读取");}
  String hash=store.encode(Arrays.asList(hospital,purpose,reason.trim(),previous,file.getOriginalFilename(),file.getContentType(),fingerprint));
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);hospital(hospital,true);
  var old=store.jdbc().queryForList("SELECT resource_id,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.requestPayload')) AS payload FROM lin_audit_record WHERE actor_user_id=? AND action='HOSPITAL_DOCUMENT_UPLOADED' AND resource_type='HOSPITAL_DOCUMENT' AND request_id=? FOR UPDATE",actor,key);
  if(!old.isEmpty()){
   if(old.size()!=1 || !hash.equals(old.get(0).get("payload")))throw new BusinessException(409,"原上传请求内容不一致，请核对已上传文件");
   long id=Long.parseLong(old.get(0).get("resource_id").toString());store.one("SELECT id FROM lin_file WHERE id=? AND hospital_id=? AND biz_type='HOSPITAL_DOCUMENT' AND deleted_at IS NULL",id,hospital);return view(id);
  }
  if(previous!=null){
   store.one("SELECT id FROM lin_file WHERE id=? AND hospital_id=? AND biz_type='HOSPITAL_DOCUMENT' AND deleted_at IS NULL FOR UPDATE",previous,hospital);
   var prior=store.one("SELECT JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.purpose')) AS purpose FROM lin_audit_record WHERE resource_type='HOSPITAL_DOCUMENT' AND action='HOSPITAL_DOCUMENT_UPLOADED' AND resource_id=?",previous.toString());
   if(!purpose.equals(prior.get("purpose")))throw new BusinessException(409,"替换文件的资料类型不一致");
   if(!store.jdbc().queryForList("SELECT id FROM lin_audit_record WHERE action='HOSPITAL_DOCUMENT_UPLOADED' AND resource_type='HOSPITAL_DOCUMENT' AND JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.previousFileId'))=? FOR UPDATE",previous.toString()).isEmpty())throw new BusinessException(409,"原文件已被替换，请刷新后选择最新版本");
  }
  long id=files.saveHospitalDocument(hospital,file);var metadata=new LinkedHashMap<String,Object>();metadata.put("purpose",purpose);metadata.put("reason",reason.trim());metadata.put("previousFileId",previous);metadata.put("requestPayload",hash);
  store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,'HOSPITAL_DOCUMENT_UPLOADED','HOSPITAL_DOCUMENT',?,?,?)",actor,Long.toString(id),key,store.encode(metadata));return view(id);
 }
 private void hospital(int id,boolean lock){if(id<=0)throw new BusinessException(400,"医院编号无效");store.one("SELECT InfoHospitalId FROM lin_lb_infohospital WHERE InfoHospitalId=?"+(lock?" FOR UPDATE":""),id);}
 private Map<String,Object> view(long id){return Map.of("id",id,"url","/platform-files/"+id+"/download");}
}
