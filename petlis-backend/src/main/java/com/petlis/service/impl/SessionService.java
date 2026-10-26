package com.petlis.service.impl;

import com.petlis.common.dto.response.WechatLoginResponse;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.util.JwtUtil;
import com.petlis.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class SessionService {
 private final FeatureStore store;
 private final JwtUtil jwt;
 private final com.petlis.common.security.ReviewPermission permission;
 public WechatLoginResponse issue(int userId){
  var u=store.one("SELECT u.id,u.openid,u.status,u.hospital_id,p.review_status,p.review_reason,COALESCE(p.credential_version,0) AS credential_version,h.HospitalName,h.CityId,h.Place,h.Tel FROM lin_user u LEFT JOIN lin_doctor_profile p ON p.user_id=u.id LEFT JOIN lin_lb_infohospital h ON h.InfoHospitalId=u.hospital_id WHERE u.id=?",userId);
  if("rejected".equals(u.get("review_status")))throw new BusinessException(403,"注册审核未通过："+u.getOrDefault("review_reason","请联系审核人员"));
  if(!"normal".equals(u.get("status")) || (u.get("review_status")!=null && !"approved".equals(u.get("review_status"))))throw new BusinessException(403,"账号尚未审核通过或已停用");
  if(permission.isReviewerId(userId))return WechatLoginResponse.builder().userId(userId).reviewer(true).token(jwt.generateReviewerToken(userId,((Number)u.get("credential_version")).intValue())).build();
  if(u.get("hospital_id")==null || u.get("CityId")==null || ((Number)u.get("CityId")).intValue()==0)throw new BusinessException(403,"医院关联信息不完整，请联系管理员");
  int hospitalId=((Number)u.get("hospital_id")).intValue(),cityId=((Number)u.get("CityId")).intValue();
  String openid=(String)u.get("openid");
  var hospital=UserInfoResponse.HospitalInfo.builder().hospitalId(hospitalId).hospitalName((String)u.get("HospitalName")).cityId(cityId).place((String)u.get("Place")).tel((String)u.get("Tel")).build();
  return WechatLoginResponse.builder().userId(userId).hospitalId(hospitalId).hospitalName((String)u.get("HospitalName")).hospital(hospital).openid(openid).token(jwt.generateToken(userId,openid,hospitalId,cityId,((Number)u.get("credential_version")).intValue())).build();
 }
 public boolean valid(String token){
  if(!jwt.validateToken(token))return false;
  var rows=store.jdbc().queryForList("SELECT u.status,u.hospital_id,p.review_status,COALESCE(p.credential_version,0) AS credential_version FROM lin_user u LEFT JOIN lin_doctor_profile p ON p.user_id=u.id WHERE u.id=?",jwt.getUserIdFromToken(token));
  if(rows.isEmpty())return false;var u=rows.get(0);
  boolean active="normal".equals(u.get("status")) && (u.get("review_status")==null || "approved".equals(u.get("review_status"))) && ((Number)u.get("credential_version")).intValue()==jwt.getCredentialVersion(token);
  if(!active)return false;
  if(jwt.isReviewerToken(token))return permission.isReviewerId(jwt.getUserIdFromToken(token));
  return !permission.isReviewerId(jwt.getUserIdFromToken(token)) && u.get("hospital_id")!=null && Objects.equals(((Number)u.get("hospital_id")).intValue(),jwt.getHospitalIdFromToken(token));
 }
 @Transactional public void revoke(int userId){
  store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,review_status,credential_version) VALUES(?,'approved',1) ON DUPLICATE KEY UPDATE credential_version=credential_version+1",userId);
 }
}
