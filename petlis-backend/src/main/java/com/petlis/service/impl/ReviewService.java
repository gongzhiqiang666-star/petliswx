package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.SecurityUtils;
import com.petlis.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class ReviewService {
 private final BusinessNotifications notifications;

 private final FeatureStore store;
 private final ReviewPermission permission;
 private final ObjectMapper mapper;
 private final OrderService orders;
 public record Decision(boolean approved,String reason){}
 private void validate(Decision r){if(r.reason()!=null && r.reason().length()>500)throw new BusinessException("审核意见过长");if(!r.approved() && (r.reason()==null || r.reason().isBlank()))throw new BusinessException("请填写驳回原因");}
 public Map<String,Object> registrations(int page,int size){permission.requireReviewer();return store.page("SELECT COUNT(*) FROM lin_doctor_profile WHERE review_status='pending'","SELECT p.user_id AS userId,u.username,u.nickname,u.mobile,p.title,p.qualification_files AS qualificationFiles,p.requested_hospital_id AS requestedHospitalId,h.HospitalName AS hospitalName,p.created_at AS createdAt FROM lin_doctor_profile p JOIN lin_user u ON u.id=p.user_id LEFT JOIN lin_lb_infohospital h ON h.InfoHospitalId=p.requested_hospital_id WHERE p.review_status='pending' ORDER BY p.created_at",page,size);}
 @Transactional public void registration(int userId,Decision r){
  permission.requireReviewer();validate(r);
  var profile=store.one("SELECT review_status,requested_hospital_id FROM lin_doctor_profile WHERE user_id=? FOR UPDATE",userId);
  if(!"pending".equals(profile.get("review_status")))throw new BusinessException("注册申请已处理");
  Object hospitalId=profile.get("requested_hospital_id");
  if(r.approved()){var h=store.one("SELECT CityId FROM lin_lb_infohospital WHERE InfoHospitalId=? AND IsActive=1",hospitalId);if(h.get("CityId")==null || ((Number)h.get("CityId")).intValue()==0)throw new BusinessException("请先维护医院城市信息");store.jdbc().update("UPDATE lin_user SET hospital_id=?,status='normal',updatetime=UNIX_TIMESTAMP() WHERE id=?",hospitalId,userId);}
  store.jdbc().update("UPDATE lin_doctor_profile SET review_status=?,review_reason=?,reviewed_by=?,reviewed_at=NOW(),credential_version=credential_version+1 WHERE user_id=?",r.approved()?"approved":"rejected",r.reason()==null?"":r.reason(),SecurityUtils.getCurrentUserId(),userId);
  notifications.emit(userId,"profile","registration:"+userId+":"+(r.approved()?"approved":"rejected"),"注册审核结果",r.approved()?"医生注册已通过审核，可以开始送检。":"医生注册未通过审核："+r.reason(),"pages/mine/mine");
 }
 public Map<String,Object> profileChanges(int page,int size){permission.requireReviewer();return store.page("SELECT COUNT(*) FROM lin_profile_change WHERE status='pending'","SELECT id,user_id AS userId,requested_data AS requestedData,previous_data AS previousData,created_at AS createdAt FROM lin_profile_change WHERE status='pending' ORDER BY id",page,size);}
 @Transactional public void profileChange(long id,Decision r){
  permission.requireReviewer();validate(r);var item=store.one("SELECT user_id,status,requested_data FROM lin_profile_change WHERE id=? FOR UPDATE",id);
  if(!"pending".equals(item.get("status")))throw new BusinessException("资料变更已处理");
  int userId=((Number)item.get("user_id")).intValue();
  if(r.approved()){
   AccountService.ProfileChange change;try{change=mapper.readValue(item.get("requested_data").toString(),AccountService.ProfileChange.class);}catch(Exception e){throw new BusinessException("资料格式无效");}
   var h=store.one("SELECT CityId FROM lin_lb_infohospital WHERE InfoHospitalId=? AND IsActive=1",change.hospitalId());if(h.get("CityId")==null || ((Number)h.get("CityId")).intValue()==0)throw new BusinessException("医院城市未维护");
   store.jdbc().update("UPDATE lin_user SET nickname=?,avatar=?,hospital_id=?,updatetime=UNIX_TIMESTAMP() WHERE id=?",change.nickname(),change.avatar(),change.hospitalId(),userId);
   store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,title,qualification_files,requested_hospital_id,review_status,reviewed_by,reviewed_at,credential_version) VALUES(?,?,?,?,'approved',?,NOW(),1) ON DUPLICATE KEY UPDATE title=VALUES(title),qualification_files=VALUES(qualification_files),requested_hospital_id=VALUES(requested_hospital_id),reviewed_by=VALUES(reviewed_by),reviewed_at=NOW(),credential_version=credential_version+1",userId,change.title(),store.encode(change.qualificationFiles()),change.hospitalId(),SecurityUtils.getCurrentUserId());
  }
  store.jdbc().update("UPDATE lin_profile_change SET status=?,review_reason=?,reviewed_by=?,reviewed_at=NOW() WHERE id=?",r.approved()?"approved":"rejected",r.reason()==null?"":r.reason(),SecurityUtils.getCurrentUserId(),id);
  notifications.emit(userId,"profile","profile:"+id,"资料审核结果",r.approved()?"资料修改已通过审核，请重新登录。":"资料修改未通过审核："+r.reason(),"pages/mine/mine");
 }
 public Map<String,Object> pendingOrders(int page,int size){permission.requireReviewer();return store.page("SELECT COUNT(*) FROM lin_lb_listdiagnosisproject WHERE SampleStatus=0","SELECT p.ListDiagnosisProjectId AS orderId,p.UserNO AS orderNo,p.AnimalName AS animalName,p.DoctorName AS doctorName,p.TestFee AS amount,h.HospitalName AS hospitalName FROM lin_lb_listdiagnosisproject p LEFT JOIN lin_lb_infohospital h ON h.InfoHospitalId=p.InfoHospitalId WHERE p.SampleStatus=0 ORDER BY p.CreateDt",page,size);}
 @Transactional public void order(int id,Decision r){permission.requireReviewer();validate(r);var row=store.one("SELECT p.SampleStatus,p.TestFee,p.CreateSampleId,h.PayType FROM lin_lb_listdiagnosisproject p JOIN lin_lb_infohospital h ON h.InfoHospitalId=p.InfoHospitalId WHERE p.ListDiagnosisProjectId=? FOR UPDATE",id);if(((Number)row.get("SampleStatus")).intValue()!=0)throw new BusinessException("送检申请已处理");int target=r.approved()?((((Number)row.get("PayType")).intValue()==1 || new java.math.BigDecimal(row.get("TestFee").toString()).signum()==0)?2:-3):1;orders.updateOrderStatus(id,target);store.jdbc().update("UPDATE lin_order_extension SET review_status=?,review_reason=?,reviewed_by=?,reviewed_at=NOW() WHERE order_id=?",r.approved()?"approved":"rejected",r.reason()==null?"":r.reason(),SecurityUtils.getCurrentUserId(),id);notifications.reviewed(id,row.get("CreateSampleId")==null?null:((Number)row.get("CreateSampleId")).intValue(),target,r.reason(),SecurityUtils.getCurrentUserId());}
 public Map<String,Object> invoices(int page,int size){permission.requireReviewer();return store.page("SELECT COUNT(*) FROM lin_invoice_application WHERE status='pending'","SELECT id,user_id AS userId,profile_snapshot AS profileSnapshot,amount,created_at AS createdAt FROM lin_invoice_application WHERE status='pending' ORDER BY id",page,size);}
}
