package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

@Component @RequiredArgsConstructor
public class SubscriptionPush {
 private final Environment env;
 private final ObjectMapper json;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 private String accessToken;private Instant tokenExpiry=Instant.EPOCH;
 public record Outcome(String status,String code,String message,boolean retryable){}
 public Outcome send(String type,String openid,String title,String message,String page){
  String prefix="app.notifications.templates."+type+".";String template=env.getProperty(prefix+"id","");String titleField=env.getProperty(prefix+"title-field","");String messageField=env.getProperty(prefix+"message-field","");
  if(!env.getProperty("app.notifications.push-enabled",Boolean.class,false) || template.isBlank() || titleField.isBlank() || messageField.isBlank())return new Outcome("unconfigured","NOT_CONFIGURED","微信推送或该业务模板尚未配置",false);
  try{
   Map<String,Object> data=new LinkedHashMap<>();data.put(titleField,Map.of("value",shortText(title)));data.put(messageField,Map.of("value",shortText(message)));String timeField=env.getProperty(prefix+"time-field","");if(!timeField.isBlank())data.put(timeField,Map.of("value",java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))));
   String body=json.writeValueAsString(Map.of("touser",openid,"template_id",template,"page",page,"data",data,"lang","zh_CN","miniprogram_state",env.getProperty("app.notifications.miniprogram-state","developer")));
   HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token="+token())).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
   var response=client.send(request,HttpResponse.BodyHandlers.ofString());if(response.statusCode()!=200)return new Outcome("failed","HTTP_ERROR","微信服务暂不可用",true);
   var result=json.readTree(response.body());if(!result.has("errcode"))return new Outcome("failed","INVALID_RESPONSE","微信返回无效响应",true);int code=result.path("errcode").asInt(-1);
   if(code==0)return new Outcome("sent","0","微信已接受推送请求",false);
   if(code==40001 || code==42001){synchronized(this){accessToken=null;tokenExpiry=Instant.EPOCH;}}
   boolean retry=Set.of(-1,40001,42001,45009).contains(code);
   return new Outcome("failed",String.valueOf(code),code==43101?"用户尚未授权订阅此类消息":"微信未接受推送，错误码 "+code,retry);
  }catch(InterruptedException e){Thread.currentThread().interrupt();return new Outcome("failed","INTERRUPTED","发送被中断",true);}catch(Exception e){return new Outcome("failed","TRANSPORT_ERROR","微信推送暂不可用",true);}
 }
 private synchronized String token()throws Exception{
  if(accessToken!=null && Instant.now().isBefore(tokenExpiry))return accessToken;
  String appId=env.getProperty("app.wechat.appid","");String secret=env.getProperty("app.wechat.secret","");if(secret.isBlank())throw new IllegalStateException("Wechat secret not configured");
  var body=json.writeValueAsString(Map.of("grant_type","client_credential","appid",appId,"secret",secret,"force_refresh",false));
  var r=client.send(HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com/cgi-bin/stable_token")).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
  var result=json.readTree(r.body());if(r.statusCode()!=200 || !result.hasNonNull("access_token"))throw new IllegalStateException("Wechat token unavailable");accessToken=result.get("access_token").asText();tokenExpiry=Instant.now().plusSeconds(Math.max(30,result.path("expires_in").asInt(300)-120));return accessToken;
 }
 static String shortText(String s){return s.codePointCount(0,s.length())<=20?s:s.substring(0,s.offsetByCodePoints(0,20));}
}
