package com.petlis.common.util;

import com.petlis.common.exception.BusinessException;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class WechatUtil {
 @Value("${app.wechat.appid}") private String appid;
 @Value("${app.wechat.secret}") private String secret;
 @Value("${app.wechat.url}") private String url;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 public boolean configured(){return secret!=null && !secret.isBlank();}
 public WechatLoginResponse code2Session(String code){
  if(code==null || code.isBlank() || code.length()>512)throw new BusinessException("微信授权凭证无效");
  if(secret==null || secret.isBlank())throw new BusinessException("微信登录尚未配置，请使用账号密码登录");
  try{
   String target=url+"?appid="+encode(appid)+"&secret="+encode(secret)+"&js_code="+encode(code)+"&grant_type=authorization_code";
   var response=client.send(HttpRequest.newBuilder(URI.create(target)).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=200)throw new BusinessException("微信登录暂不可用，请稍后重试");
   var data=JSONUtil.parseObj(response.body());
   if(data.containsKey("errcode") && data.getInt("errcode",-1)!=0)throw new BusinessException("微信授权已失效，请重新授权");
   String openid=data.getStr("openid"),sessionKey=data.getStr("session_key");
   if(openid==null || openid.isBlank() || sessionKey==null || sessionKey.isBlank())throw new BusinessException("微信授权响应无效，请重试");
   return new WechatLoginResponse(openid,sessionKey);
  }catch(BusinessException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new BusinessException("微信登录已中断，请重试");}catch(Exception e){throw new BusinessException("微信登录暂不可用，请稍后重试");}
 }
 private static String encode(String v){return URLEncoder.encode(v,StandardCharsets.UTF_8);}
 public record WechatLoginResponse(String openid,String sessionKey){}
}
