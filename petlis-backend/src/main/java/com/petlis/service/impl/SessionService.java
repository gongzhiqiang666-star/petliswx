package com.petlis.service.impl;

import com.petlis.common.dto.response.WechatLoginResponse;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.PlatformRole;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.UserRoleService;
import com.petlis.common.util.JwtUtil;
import com.petlis.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class SessionService {
 private final FeatureStore store;
 private final JwtUtil jwt;
 private final ReviewPermission permission;
 private final UserRoleService userRoles;

 public Set<PlatformRole> roles(int userId) { return userRoles.roles(userId); }

 public WechatLoginResponse issue(int userId){
  var u=store.one("SELECT u.id,u.username,u.nickname,u.mobile,u.openid,u.status,u.hospital_id,p.review_status,p.review_reason,COALESCE(p.credential_version,0) AS credential_version,h.HospitalName,h.CityId,h.Place,h.Tel FROM lin_user u LEFT JOIN lin_doctor_profile p ON p.user_id=u.id LEFT JOIN lin_lb_infohospital h ON h.InfoHospitalId=u.hospital_id WHERE u.id=?",userId);
  if("rejected".equals(u.get("review_status")))throw new BusinessException(403,"注册审核未通过："+u.getOrDefault("review_reason","请联系审核人员"));
  if(!"normal".equals(u.get("status")) || (u.get("review_status")!=null && !"approved".equals(u.get("review_status"))))throw new BusinessException(403,"账号尚未审核通过或已停用");
  Set<PlatformRole> assigned=userRoles.roles(userId);
  boolean legacyReviewer=permission.isLegacyReviewerId(userId);
  Set<String> roleNames=new LinkedHashSet<>(assigned.isEmpty()?Set.of(legacyReviewer?"REVIEWER":PlatformRole.DOCTOR.name()):assigned.stream().map(Enum::name).collect(Collectors.toSet()));
  if(legacyReviewer)roleNames.add("REVIEWER");
  roleNames=Set.copyOf(roleNames);
  Map<String,Object> user=new LinkedHashMap<>();user.put("id",userId);user.put("username",u.get("username"));user.put("displayName",u.get("nickname"));user.put("phone",u.get("mobile"));user.put("hospitalId",u.get("hospital_id"));user.put("roles",roleNames);
  boolean reviewer=legacyReviewer || assigned.contains(PlatformRole.ADMIN) && u.get("hospital_id")==null;
  if(reviewer)return WechatLoginResponse.builder().userId(userId).reviewer(true).roles(roleNames).user(user).token(jwt.generateReviewerToken(userId,((Number)u.get("credential_version")).intValue())).build();
  if(u.get("hospital_id")==null || u.get("CityId")==null || ((Number)u.get("CityId")).intValue()==0)throw new BusinessException(403,"医院关联信息不完整，请联系管理员");
  int hospitalId=((Number)u.get("hospital_id")).intValue(),cityId=((Number)u.get("CityId")).intValue();
  String openid=(String)u.get("openid");
  var hospital=UserInfoResponse.HospitalInfo.builder().hospitalId(hospitalId).hospitalName((String)u.get("HospitalName")).cityId(cityId).place((String)u.get("Place")).tel((String)u.get("Tel")).build();
  return WechatLoginResponse.builder().userId(userId).hospitalId(hospitalId).hospitalName((String)u.get("HospitalName")).hospital(hospital).openid(openid).roles(roleNames).user(user).token(jwt.generateToken(userId,openid,hospitalId,cityId,((Number)u.get("credential_version")).intValue())).build();
 }
 public boolean valid(String token){
  if(!jwt.validateToken(token))return false;
  int userId=jwt.getUserIdFromToken(token);
  var rows=store.jdbc().queryForList("SELECT u.status,u.hospital_id,p.review_status,COALESCE(p.credential_version,0) AS credential_version FROM lin_user u LEFT JOIN lin_doctor_profile p ON p.user_id=u.id WHERE u.id=?",userId);
  if(rows.isEmpty())return false;var u=rows.get(0);
  boolean active="normal".equals(u.get("status")) && (u.get("review_status")==null || "approved".equals(u.get("review_status"))) && ((Number)u.get("credential_version")).intValue()==jwt.getCredentialVersion(token);
  if(!active)return false;
  if(jwt.isReviewerToken(token))return permission.isReviewerId(userId);
  return !permission.isLegacyReviewerId(userId) && u.get("hospital_id")!=null && jwt.getHospitalIdFromToken(token)!=null && Objects.equals(((Number)u.get("hospital_id")).intValue(),jwt.getHospitalIdFromToken(token));
 }
 @Transactional public void revoke(int userId){
  store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,review_status,credential_version) VALUES(?,'approved',1) ON DUPLICATE KEY UPDATE credential_version=credential_version+1",userId);
 }
}
