package com.petlis.controller.api;
import com.petlis.common.response.Result;
import com.petlis.common.dto.response.WechatLoginResponse;
import com.petlis.service.impl.DoctorAuthService;
import com.petlis.service.impl.SmsVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("auth") @RequiredArgsConstructor
public class DoctorAuthController {
 private final DoctorAuthService service;
 private final SmsVerificationService sms;
 private final org.springframework.jdbc.core.JdbcTemplate jdbc;
 private final com.petlis.common.util.WechatUtil wechat;
 @GetMapping("/capabilities") public Result<java.util.Map<String,Object>> capabilities(){return Result.success(java.util.Map.of("smsConfigured",sms.configured(),"wechatConfigured",wechat.configured()));}
 @GetMapping("/hospitals") public Result<java.util.List<java.util.Map<String,Object>>> hospitals(@RequestParam(defaultValue="") String q){
  if(q.length()>100)throw new com.petlis.common.exception.BusinessException("搜索词过长");
  return Result.success(jdbc.queryForList("SELECT InfoHospitalId AS hospitalId,HospitalName AS hospitalName,CityId AS cityId FROM lin_lb_infohospital WHERE IsActive=1 AND HospitalName LIKE ? ORDER BY HospitalName LIMIT 100","%"+q+"%"));
 }
 @PostMapping("/password/reset") public Result<Void> reset(@RequestBody DoctorAuthService.PasswordReset r){service.resetPassword(r);return Result.success("密码已重置，请重新登录");}
 @PostMapping("/mobile/change") public Result<Void> mobile(@RequestBody DoctorAuthService.MobileChange r){service.changeMobile(r);return Result.success("手机号已修改，请重新登录");}
 @PostMapping("/register") public Result<Long> register(@RequestBody DoctorAuthService.Registration r){return Result.success("注册资料已提交，审核通过后可登录",service.register(r));}
 @PostMapping("/password/login") public Result<WechatLoginResponse> login(@RequestBody DoctorAuthService.Login r){return Result.success(service.login(r));}
 @PostMapping("/password/change") public Result<Void> password(@RequestBody DoctorAuthService.PasswordChange r){service.changePassword(r);return Result.success("密码已修改，请重新登录");}
 public record BindWechat(String code){}
 @PostMapping("/wechat/bind") public Result<Void> bind(@RequestBody BindWechat r){service.bindWechat(r.code());return Result.success("绑定成功，请重新登录");}
 public record SendSms(String mobile,String scene){}
 @PostMapping("/sms/send") public Result<Void> send(@RequestBody SendSms r,HttpServletRequest request){sms.send(r.mobile(),r.scene(),request.getRemoteAddr());return Result.success("验证码已发送");}
}
