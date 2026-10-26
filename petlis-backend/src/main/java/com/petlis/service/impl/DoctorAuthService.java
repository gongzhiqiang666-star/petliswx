package com.petlis.service.impl;

import com.petlis.common.dto.response.WechatLoginResponse;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import com.petlis.common.util.WechatUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.util.*;

@Service @RequiredArgsConstructor
public class DoctorAuthService {
 private final FeatureStore store;
 private final SessionService sessions;
 private final SmsVerificationService sms;
 private final WechatUtil wechat;
 private final QualificationStorage qualifications;
 private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
 public record Registration(String username,String password,String mobile,String code,Integer hospitalId,String nickname,String title,List<String> qualificationFiles,String wechatCode){}
 @Transactional public long register(Registration r){
  if(r.username()==null || !r.username().matches("[A-Za-z0-9_]{4,32}") || r.nickname()==null || r.nickname().isBlank() || r.nickname().length()>50 || r.hospitalId()==null || r.title()==null || r.title().length()>100 || r.mobile()==null || !r.mobile().matches("1[0-9]{10}"))throw new BusinessException("请完整填写账号、姓名、手机号、医院和职称");
  validatePassword(r.password());
  qualifications.validate(r.qualificationFiles());
  lockRegistration(r.username(),r.mobile());
  if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_user WHERE username=? OR mobile=? OR mobile=? OR username=?)",Boolean.class,r.username(),r.mobile(),r.username(),r.mobile())))throw new BusinessException("账号或手机号已注册，请直接登录");
  store.one("SELECT InfoHospitalId FROM lin_lb_infohospital WHERE InfoHospitalId=? AND IsActive=1",r.hospitalId());
  sms.consume(r.mobile(),"register",r.code());
  String openid=r.wechatCode()==null || r.wechatCode().isBlank()?null:wechat.code2Session(r.wechatCode()).openid();
  long id=store.insert("INSERT INTO lin_user(group_id,username,nickname,mobile,openid,password,salt,money,score,level,gender,successions,maxsuccessions,loginfailure,status,createtime,updatetime,jointime) VALUES(0,?,?,?,?, '', '',0,0,0,0,0,0,0,'hidden',UNIX_TIMESTAMP(),UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",r.username(),r.nickname(),r.mobile(),openid);
  store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,password_hash,title,qualification_files,requested_hospital_id,review_status) VALUES(?,?,?,?,?,'pending')",id,encoder.encode(r.password()),r.title(),store.encode(r.qualificationFiles()),r.hospitalId());
  return id;
 }
 public record Login(String account,String password){}
 @Transactional(noRollbackFor=BusinessException.class) public WechatLoginResponse login(Login r){
  if(r.account()==null || r.account().isBlank() || r.account().length()>100 || r.password()==null || r.password().length()>200)throw new BusinessException("账号或密码不正确");
  var rows=store.jdbc().queryForList("SELECT u.*,p.password_hash,p.locked_until,p.review_status FROM lin_user u LEFT JOIN lin_doctor_profile p ON p.user_id=u.id WHERE u.username=? OR u.mobile=? ORDER BY u.id LIMIT 2 FOR UPDATE",r.account(),r.account());
  if(rows.size()!=1){encoder.matches(r.password(),"$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW");throw new BusinessException("账号或密码不正确");}
  var u=rows.get(0);int id=((Number)u.get("id")).intValue();
  if(u.get("locked_until") instanceof Timestamp t && t.toInstant().isAfter(java.time.Instant.now()))throw new BusinessException(429,"账号暂时锁定，请稍后重试");
  String hash=(String)u.get("password_hash");
  boolean valid=hash!=null && !hash.isBlank()?encoder.matches(r.password(),hash):legacyMatches(r.password(),(String)u.get("salt"),(String)u.get("password"));
  if(!valid){int failures=((Number)u.get("loginfailure")).intValue()+1;store.jdbc().update("UPDATE lin_user SET loginfailure=LEAST(?,255) WHERE id=?",failures,id);if(failures>=5)store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,review_status,locked_until) VALUES(?,'approved',DATE_ADD(NOW(),INTERVAL 15 MINUTE)) ON DUPLICATE KEY UPDATE locked_until=DATE_ADD(NOW(),INTERVAL 15 MINUTE)",id);throw new BusinessException("账号或密码不正确");}
  // 先验证审核与医院归属，避免给未审核账号签发令牌。
  WechatLoginResponse response=sessions.issue(id);
  store.jdbc().update("UPDATE lin_user SET loginfailure=0,prevtime=logintime,logintime=UNIX_TIMESTAMP() WHERE id=?",id);
  if(hash==null || hash.isBlank())store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,password_hash,review_status) VALUES(?,?,'approved') ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash),locked_until=NULL",id,encoder.encode(r.password()));
  else store.jdbc().update("UPDATE lin_doctor_profile SET locked_until=NULL WHERE user_id=?",id);
  return response;
 }
 public record PasswordChange(String oldPassword,String newPassword){}
 @Transactional public void changePassword(PasswordChange r){
  if(r.oldPassword()==null || r.oldPassword().isBlank())throw new BusinessException("请输入原密码");
  validatePassword(r.newPassword());int id=SecurityUtils.getCurrentUserId();
  var u=store.one("SELECT u.password,u.salt,p.password_hash FROM lin_user u LEFT JOIN lin_doctor_profile p ON p.user_id=u.id WHERE u.id=? FOR UPDATE",id);
  String hash=(String)u.get("password_hash");boolean valid=hash!=null?encoder.matches(r.oldPassword(),hash):legacyMatches(r.oldPassword(),(String)u.get("salt"),(String)u.get("password"));
  if(!valid)throw new BusinessException("原密码不正确");
  store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,password_hash,review_status,credential_version) VALUES(?,?,'approved',1) ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash),credential_version=credential_version+1",id,encoder.encode(r.newPassword()));
 }
 @Transactional public void bindWechat(String code){
  if(code==null || code.isBlank())throw new BusinessException("微信授权凭证不能为空");
  String openid=wechat.code2Session(code).openid();int id=SecurityUtils.getCurrentUserId();
  if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_user WHERE openid=? AND id<>?)",Boolean.class,openid,id)))throw new BusinessException("该微信已绑定其他医生账号");
  store.jdbc().update("UPDATE lin_user SET openid=?,updatetime=UNIX_TIMESTAMP() WHERE id=?",openid,id);sessions.revoke(id);
 }
 public record PasswordReset(String mobile,String code,String newPassword){}
 @Transactional public void resetPassword(PasswordReset r){
  validatePassword(r.newPassword());
  if(r.mobile()==null || !r.mobile().matches("1[0-9]{10}"))throw new BusinessException("手机号无效");
  var rows=store.jdbc().queryForList("SELECT id FROM lin_user WHERE mobile=? ORDER BY id LIMIT 2 FOR UPDATE",r.mobile());
  if(rows.size()!=1)throw new BusinessException("无法重置该账号，请联系管理员");
  sms.consume(r.mobile(),"reset",r.code());int id=((Number)rows.get(0).get("id")).intValue();
  store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,password_hash,review_status,credential_version) VALUES(?,?,'approved',1) ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash),credential_version=credential_version+1,locked_until=NULL",id,encoder.encode(r.newPassword()));
  store.jdbc().update("UPDATE lin_user SET loginfailure=0 WHERE id=?",id);
 }
 public record MobileChange(String mobile,String code){}
 @Transactional public void changeMobile(MobileChange r){
  if(r.mobile()==null || !r.mobile().matches("1[0-9]{10}"))throw new BusinessException("手机号无效");
  int id=SecurityUtils.getCurrentUserId();lockRegistration("mobile-change",r.mobile());
  store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",id);
  if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_user WHERE (mobile=? OR username=?) AND id<>?)",Boolean.class,r.mobile(),r.mobile(),id)))throw new BusinessException("该手机号已绑定其他账号");
  sms.consume(r.mobile(),"bind",r.code());store.jdbc().update("UPDATE lin_user SET mobile=?,updatetime=UNIX_TIMESTAMP() WHERE id=?",r.mobile(),id);sessions.revoke(id);
 }
 static boolean legacyMatches(String password,String salt,String expected){
  if(password==null || expected==null || expected.isBlank())return false;
  try{MessageDigest md=MessageDigest.getInstance("MD5");String first=HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));String result=HexFormat.of().formatHex(md.digest((first+(salt==null?"":salt)).getBytes(StandardCharsets.UTF_8)));return MessageDigest.isEqual(result.getBytes(StandardCharsets.US_ASCII),expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 private void validatePassword(String value){if(value==null || value.length()<8 || value.getBytes(StandardCharsets.UTF_8).length>72 || !value.matches("(?s).*[A-Za-z].*") || !value.matches("(?s).*[0-9].*"))throw new BusinessException("密码需至少8位，包含字母和数字，且不超过72字节");}
 private void lockRegistration(String username,String mobile){
  var keys=new ArrayList<>(List.of("petlis-account:"+username.toLowerCase(Locale.ROOT),"petlis-account:"+mobile));Collections.sort(keys);
  for(String key:keys){Integer acquired=store.jdbc().queryForObject("SELECT GET_LOCK(?,5)",Integer.class,key);if(!Integer.valueOf(1).equals(acquired))throw new BusinessException(409,"注册请求处理中，请稍后重试");TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){store.jdbc().queryForObject("SELECT RELEASE_LOCK(?)",Integer.class,key);}});}
 }
}
