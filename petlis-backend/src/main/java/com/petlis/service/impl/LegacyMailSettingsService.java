package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** PHP auth/LbDictmailserver maintenance. Saving settings never opens an SMTP connection. */
@Service
public class LegacyMailSettingsService {
 private final FeatureStore store;
 private final AccessService access;
 private final byte[] signingKey;
 public LegacyMailSettingsService(FeatureStore store,AccessService access,@Value("${app.jwt.secret}") String secret){this.store=store;this.access=access;this.signingKey=secret.getBytes(StandardCharsets.UTF_8);}
 public record Command(String host,Integer port,String username,String password,String bcc,String requestKey,String expectedToken){
  @Override public String toString(){return "MailSettingsCommand[password=REDACTED]";}
 }
 public record DeleteCommand(String requestKey,String expectedToken){}
 private boolean[] policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-mail-settings' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));
  if(rows.size()!=1)return new boolean[]{false,false,false};
  try{var j=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=j.path("enabled").isBoolean()&&j.path("enabled").asBoolean(),write=read&&j.path("writeEnabled").isBoolean()&&j.path("writeEnabled").asBoolean();return new boolean[]{read,write,write&&j.path("deleteEnabled").isBoolean()&&j.path("deleteEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"邮件配置参数无效");}
 }
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("mailSettings",p[0],"mailSettingsWrites",p[1],"mailSettingsDeletes",p[2]);}
 private void read(){if(!policy(false)[0])throw new BusinessException(409,"邮件配置维护尚未开启");}
 private void id(long id){if(id<1||id>2147483647L)throw new BusinessException(400,"邮件配置编号无效");}
 private Map<String,Object> raw(long id,boolean lock){id(id);return new LinkedHashMap<>(store.one("SELECT DictMailServerId,ServerHost,Port,UserName,Psw,BCCMail FROM lin_lb_dictmailserver WHERE DictMailServerId=?"+(lock?" FOR UPDATE":""),id));}
 private Map<String,Object> publicRow(Map<String,Object> raw){var result=new LinkedHashMap<>(raw);Object secret=result.remove("Psw");result.put("passwordConfigured",secret!=null&&!secret.toString().isEmpty());return result;}
 private String fingerprint(Object value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(signingKey,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(("petlis-mail-settings-v1:"+store.encode(value)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("邮件配置校验失败",e);}}
 private String token(Map<String,Object> row){return fingerprint(new TreeMap<>(row));}
 @Transactional(readOnly=true) public Map<String,Object> detail(long id){read();var raw=raw(id,false);var result=publicRow(raw);result.put("editToken",token(raw));return result;}
 @Transactional(readOnly=true) public Map<String,Object> list(String q,int page,int size){read();if(page<0||size<1||size>100||q!=null&&q.length()>1000)throw new BusinessException(400,"邮件配置查询无效");String term="%"+(q==null?"":q)+"%",where=" FROM lin_lb_dictmailserver WHERE ServerHost LIKE ? OR UserName LIKE ? OR BCCMail LIKE ?";return store.page("SELECT COUNT(*)"+where,"SELECT DictMailServerId,ServerHost,Port,UserName,BCCMail,(Psw<>'') AS passwordConfigured"+where+" ORDER BY DictMailServerId DESC",page,size,term,term,term);}
 private void request(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"邮件配置请求号无效");}
 private void version(String value){if(value==null||!value.matches("[a-f0-9]{64}"))throw new BusinessException(400,"请重新打开邮件配置后修改");}
 private Long replay(int actor,String key,String fingerprint){var rows=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_MAIL_SETTINGS' AND request_id=? ORDER BY id DESC LIMIT 1",actor,key);if(rows.isEmpty())return null;try{var j=new ObjectMapper().readTree(rows.get(0).get("after_json").toString());if(!fingerprint.equals(j.path("requestFingerprint").asText()))throw new BusinessException(409,"请求号已用于其他邮件配置操作");return j.path("settingsId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"邮件配置请求无法核对");}}
 private void audit(int actor,String action,long id,String key,Map<String,Object> before,String fingerprint){store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,?,'LEGACY_MAIL_SETTINGS',?,?,?,?)",actor,action,String.valueOf(id),key,before==null?null:store.encode(publicRow(before)),store.encode(Map.of("settingsId",id,"requestFingerprint",fingerprint)));}
 private void validate(Command c,boolean create){
  if(c==null)throw new BusinessException(400,"邮件配置字段缺失");
  for(String value:new String[]{c.host(),c.username()})if(value==null||value.isBlank()||value.codePointCount(0,value.length())>250||value.indexOf('\r')>=0||value.indexOf('\n')>=0||value.indexOf('\0')>=0)throw new BusinessException(400,"服务器和用户名必填且不超过250字，不能含控制字符");
  if(c.port()==null||c.port()<1||c.port()>65535)throw new BusinessException(400,"SMTP 端口须为1至65535");
  if(create&&(c.password()==null||c.password().isEmpty())||c.password()!=null&&(c.password().codePointCount(0,c.password().length())>250||c.password().indexOf('\0')>=0))throw new BusinessException(400,"新增时须填写密码，密码不超过250字");
  if(c.bcc()==null||c.bcc().isBlank()||c.bcc().getBytes(StandardCharsets.UTF_8).length>65535||c.bcc().indexOf('\r')>=0||c.bcc().indexOf('\n')>=0||c.bcc().indexOf('\0')>=0)throw new BusinessException(400,"密送地址必填，不能含控制字符或超过字段长度");
 }
 @Transactional public long save(Long id,Command c){
  if(!policy(true)[1])throw new BusinessException(409,"邮件配置写入尚未开启");validate(c,id==null);request(c.requestKey());if(id!=null)version(c.expectedToken());int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  // Fixed property order, keyed fingerprint: no password or offline password hash in API/audit.
  var payload=new LinkedHashMap<String,Object>();payload.put("operation",id==null?"CREATE":"UPDATE");payload.put("id",id==null?0:id);payload.put("command",c);String fingerprint=fingerprint(payload);var prior=replay(actor,c.requestKey(),fingerprint);if(prior!=null)return prior;
  Map<String,Object> before=null;long result;
  if(id==null){Long max=store.jdbc().queryForObject("SELECT COALESCE(MAX(DictMailServerId),0) FROM lin_lb_dictmailserver",Long.class);result=Math.max(0,max==null?0:max)+1;id(result);try{store.jdbc().update("INSERT INTO lin_lb_dictmailserver(DictMailServerId,ServerHost,Port,UserName,Psw,BCCMail) VALUES(?,?,?,?,?,?)",result,c.host(),c.port(),c.username(),c.password(),c.bcc());}catch(org.springframework.dao.DuplicateKeyException e){throw new BusinessException(409,"邮件配置编号被其他系统占用，请重新打开后重试");}}
  else{before=raw(id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"邮件配置已变更，请重新打开核对");result=id;String password=c.password()==null||c.password().isEmpty()?before.get("Psw").toString():c.password();store.jdbc().update("UPDATE lin_lb_dictmailserver SET ServerHost=?,Port=?,UserName=?,Psw=?,BCCMail=? WHERE DictMailServerId=?",c.host(),c.port(),c.username(),password,c.bcc(),id);}
  audit(actor,id==null?"LEGACY_MAIL_SETTINGS_CREATED":"LEGACY_MAIL_SETTINGS_UPDATED",result,c.requestKey(),before,fingerprint);return result;
 }
 @Transactional public long delete(long id,DeleteCommand c){
  if(!policy(true)[2])throw new BusinessException(409,"邮件配置删除尚未开启");id(id);if(c==null)throw new BusinessException(400,"删除请求无效");request(c.requestKey());version(c.expectedToken());int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);String fingerprint=fingerprint(Map.of("operation","DELETE","id",id,"command",c));var prior=replay(actor,c.requestKey(),fingerprint);if(prior!=null)return prior;
  var before=raw(id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"邮件配置已变更，请重新打开核对");store.jdbc().update("DELETE FROM lin_lb_dictmailserver WHERE DictMailServerId=?",id);audit(actor,"LEGACY_MAIL_SETTINGS_DELETED",id,c.requestKey(),before,fingerprint);return id;
 }
}
