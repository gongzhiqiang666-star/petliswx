package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.util.*;

@Service @RequiredArgsConstructor
public class SmsVerificationService {
 private final FeatureStore store;
 private final ObjectMapper json;
 @Value("${app.sms.api-url:}") private String apiUrl;
 @Value("${app.sms.api-token:}") private String apiToken;
 private static final SecureRandom RANDOM=new SecureRandom();
 public boolean configured(){return apiUrl!=null && apiUrl.startsWith("https://") && apiToken!=null && !apiToken.isBlank();}
 public synchronized void send(String mobile,String scene,String ip){
  validate(mobile,scene);
  if(apiUrl==null || !apiUrl.startsWith("https://") || apiToken==null || apiToken.isBlank())throw new BusinessException("短信服务尚未配置，请联系管理员");
  Long recent=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_sms WHERE mobile=? AND event=? AND createtime>UNIX_TIMESTAMP()-60",Long.class,mobile,"petlis_"+scene);
  Long daily=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_sms WHERE (mobile=? OR ip=?) AND event LIKE 'petlis_%' AND createtime>UNIX_TIMESTAMP()-86400",Long.class,mobile,ip);
  if((recent!=null && recent>0) || (daily!=null && daily>=20))throw new BusinessException(429,"验证码发送过于频繁，请稍后重试");
  String code=String.format(java.util.Locale.ROOT,"%06d",RANDOM.nextInt(1000000));
  try{
   var request=HttpRequest.newBuilder(URI.create(apiUrl)).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("Authorization","Bearer "+apiToken).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("mobile",mobile,"code",code,"scene",scene)))).build();
   var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
   var response=client.send(request,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()/100!=2 || !json.readTree(response.body()).path("success").asBoolean(false))throw new BusinessException("验证码发送失败，请稍后重试");
  }catch(BusinessException e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new BusinessException("短信服务暂不可用");}
  store.jdbc().update("INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES(?,?,?,0,?,UNIX_TIMESTAMP())","petlis_"+scene,mobile,code,ip);
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW,noRollbackFor=BusinessException.class)
 public void consume(String mobile,String scene,String code){
  validate(mobile,scene);
  var rows=store.jdbc().queryForList("SELECT id,code,times FROM lin_sms WHERE mobile=? AND event=? AND createtime>UNIX_TIMESTAMP()-300 ORDER BY id DESC LIMIT 1 FOR UPDATE",mobile,"petlis_"+scene);
  if(rows.isEmpty() || code==null || !code.matches("[0-9]{6}"))throw new BusinessException("验证码无效或已过期");
  var record=rows.get(0);if(((Number)record.get("times")).intValue()>=5)throw new BusinessException("验证码尝试次数过多，请重新获取");
  if(!MessageDigest.isEqual(String.valueOf(record.get("code")).getBytes(StandardCharsets.US_ASCII),code.getBytes(StandardCharsets.US_ASCII))){store.jdbc().update("UPDATE lin_sms SET times=times+1 WHERE id=?",record.get("id"));throw new BusinessException("验证码不正确");}
  store.jdbc().update("DELETE FROM lin_sms WHERE mobile=? AND event=?",mobile,"petlis_"+scene);
 }
 private void validate(String mobile,String scene){if(mobile==null || !mobile.matches("1[0-9]{10}") || scene==null || !Set.of("register","bind","reset").contains(scene))throw new BusinessException("手机号或验证场景无效");}
}
