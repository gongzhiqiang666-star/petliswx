package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
@Service @RequiredArgsConstructor
public class QualificationOwnershipService {
 private final FeatureStore store;
 private static final SecureRandom RANDOM=new SecureRandom();
 private String name(String reference){if(reference==null || !reference.matches("/files/qualifications/[a-f0-9]{32}\\.(pdf|png|jpg)"))throw new BusinessException(400,"资质文件地址无效");return reference.substring(reference.lastIndexOf('/')+1);}
 private String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("上传凭据校验不可用");}}
 @Transactional public Map<String,String> uploaded(String reference,int actor){
  String file=name(reference),token="";var metadata=new LinkedHashMap<String,Object>();metadata.put("ownerUserId",actor);
  if(actor==0){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);metadata.put("tokenHash",digest(token));metadata.put("expiresAt",System.currentTimeMillis()+2*3600000L);}
  store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,after_json) VALUES(?,'QUALIFICATION_UPLOADED','QUALIFICATION',?,?)",actor,file,store.encode(metadata));
  return token.isEmpty()?Map.of("url",reference):Map.of("url",reference,"uploadToken",token);
 }
 @Transactional public void claim(List<String> references,Map<String,String> tokens,int user){
  if(user<=0 || references==null || references.isEmpty() || references.size()>5 || tokens==null || tokens.size()!=references.size() || !tokens.keySet().equals(new HashSet<>(references)))throw new BusinessException(400,"请重新上传资质并提供完整上传凭据");
  for(String reference:new TreeSet<>(references)){
   String file=name(reference),token=tokens.get(reference);if(token==null || !token.matches("[A-Za-z0-9_-]{43}"))throw new BusinessException(400,"资质上传凭据无效，请重新上传");
   var uploads=store.jdbc().queryForList("SELECT id,actor_user_id,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.tokenHash')) AS tokenHash,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.expiresAt')) AS expiresAt FROM lin_audit_record WHERE resource_type='QUALIFICATION' AND resource_id=? AND action='QUALIFICATION_UPLOADED' FOR UPDATE",file);
   if(uploads.size()!=1 || ((Number)uploads.get(0).get("actor_user_id")).intValue()!=0)throw new BusinessException(403,"该资质不能用于新账号注册，请重新上传");
   var upload=uploads.get(0);long expires;try{expires=Long.parseLong(Objects.toString(upload.get("expiresAt"),"0"));}catch(NumberFormatException e){expires=0;}
   if(expires<System.currentTimeMillis() || !MessageDigest.isEqual(digest(token).getBytes(StandardCharsets.US_ASCII),Objects.toString(upload.get("tokenHash"),"").getBytes(StandardCharsets.US_ASCII)))throw new BusinessException(403,"资质上传凭据已失效，请重新上传");
   var claimed=store.jdbc().queryForList("SELECT actor_user_id FROM lin_audit_record WHERE resource_type='QUALIFICATION' AND resource_id=? AND action='QUALIFICATION_CLAIMED' FOR UPDATE",file);
   if(!claimed.isEmpty())throw new BusinessException(409,"资质文件已被注册申请使用，请重新上传");
   store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,after_json) VALUES(?,'QUALIFICATION_CLAIMED','QUALIFICATION',?,?)",user,file,store.encode(Map.of("ownerUserId",user)));
  }
 }
 public void requireOwner(String reference,int user){
  String file=name(reference);
  var rows=store.jdbc().queryForList("SELECT actor_user_id FROM lin_audit_record WHERE resource_type='QUALIFICATION' AND resource_id=? AND action IN ('QUALIFICATION_UPLOADED','QUALIFICATION_CLAIMED') ORDER BY id DESC LIMIT 1",file);
  if(!rows.isEmpty()){
   if(((Number)rows.get(0).get("actor_user_id")).intValue()==user && user>0)return;
   throw new BusinessException(403,"资质文件不属于当前账号");
  }
  // Only previously approved references retain legacy compatibility; never infer ownership from a URL alone.
  if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_doctor_profile WHERE user_id=? AND review_status='approved' AND JSON_CONTAINS(qualification_files,JSON_QUOTE(?)))",Boolean.class,user,reference)))return;
  throw new BusinessException(403,"资质文件尚未绑定本人，请重新上传");
 }
 public void validateOwner(List<String> references,int user){for(String reference:references)requireOwner(reference,user);}
}
