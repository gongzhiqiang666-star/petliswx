package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original project/LbListdiagnosisproject::sendmail, with durable attempts.
 * Every network call is outside DB transactions. There is no background worker.
 */
@Service
public class LegacySampleNoticeService {
 private final FeatureStore store;private final AccessService access;private final LegacyOrderDetailService renderer;
 private final LegacyMailTransport transport;private final TransactionTemplate tx;private final int profileId;
 private final String overrideTo;private final byte[] signingKey;
 public LegacySampleNoticeService(FeatureStore store,AccessService access,LegacyOrderDetailService renderer,LegacyMailTransport transport,PlatformTransactionManager manager,@Value("${app.legacy-mail.profile-id:1}") int profileId,@Value("${app.legacy-mail.override-to:}") String overrideTo,@Value("${app.jwt.secret}") String secret){this.store=store;this.access=access;this.renderer=renderer;this.transport=transport;this.tx=new TransactionTemplate(manager);this.profileId=profileId;this.overrideTo=overrideTo;this.signingKey=secret.getBytes(StandardCharsets.UTF_8);}
 public record Command(long contactId,String title,String html,String requestKey,String expectedToken){}
 public record ReconcileCommand(String resolution,String note,boolean evidenceConfirmed,boolean workerStoppedConfirmed,String expectedToken,String requestKey){}
 private boolean[] policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-sample-notice' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));
  if(rows.size()!=1)return new boolean[]{false,false,false,false};
  try{var j=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=flag(j,"enabled"),write=read&&flag(j,"writeEnabled");return new boolean[]{read,write,write&&flag(j,"sendEnabled"),write&&flag(j,"reconcileEnabled")};}catch(Exception e){throw error(409,"收样通知参数无效");}
 }
 private boolean flag(com.fasterxml.jackson.databind.JsonNode node,String key){return node.path(key).isBoolean()&&node.path(key).asBoolean();}
 public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("sampleNotice",p[0],"sampleNoticeWrites",p[1],"sampleNoticeSend",p[2]&&transport.available(),"sampleNoticeReconcile",p[3]);}
 private void read(){if(!policy(false)[0])throw error(409,"收样通知尚未开启");}
 private Map<String,Object> order(long id,boolean lock){if(id<1||id>Integer.MAX_VALUE)throw error(400,"送检单编号无效");return store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?"+(lock?" FOR UPDATE":""),id);}
 private List<Map<String,Object>> contacts(Map<String,Object> order,boolean lock){return store.jdbc().queryForList("SELECT InfoHospitalContactId,Name,Mail FROM lin_lb_infohospitalcontact WHERE InfoHospitalId=? ORDER BY InfoHospitalContactId"+(lock?" FOR UPDATE":""),order.get("InfoHospitalId"));}
 private String fingerprint(Object value){try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(signingKey,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(("sample-notice-v1:"+store.encode(value)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("收样通知校验失败");}}
 private String version(Map<String,Object> row,List<Map<String,Object>> contacts){return fingerprint(new TreeMap<>(Map.of("order",new TreeMap<>(row),"contacts",contacts)));}
 public Map<String,Object> options(long id,int page,int size){read();var row=order(id,false);if(page<0||size<1||size>100)throw error(400,"分页无效");var templates=store.page("SELECT COUNT(*) FROM lin_lb_dictmailreportnotice","SELECT id,tplname,is_active FROM lin_lb_dictmailreportnotice ORDER BY id",page,size);@SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)templates.get("items");for(var item:items)item.put("tplname",LegacyDictionarySelector.display(item.get("tplname")));return Map.of("contacts",contacts(row,false),"templates",templates);}
 public Map<String,Object> draft(long id,long template){read();var row=order(id,false);var source=store.one("SELECT tpltext FROM lin_lb_dictmailreportnotice WHERE id=?",template);return Map.of("orderId",id,"hospitalId",row.get("InfoHospitalId"),"title","样本收到通知","html",renderer.renderSampleNoticeBody(id,Objects.toString(source.get("tpltext"),"")),"editToken",version(row,contacts(row,false)));}
 private void validate(Command c){
  if(c==null||c.contactId()<1||c.title()==null||c.title().isBlank()||c.title().length()>2000||c.title().indexOf('\r')>=0||c.title().indexOf('\n')>=0||c.title().indexOf('\0')>=0||c.html()==null||c.html().isBlank()||c.html().getBytes(StandardCharsets.UTF_8).length>2*1024*1024)throw error(400,"联系人、标题或正文无效");
  if(c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}")||c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}"))throw error(400,"请重新打开收样通知后提交");
 }
 public long queue(long orderId,Command c){return Objects.requireNonNull(tx.execute(status->{
  if(!policy(true)[1])throw error(409,"收样通知创建尚未开启");validate(c);int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String hash=fingerprint(new TreeMap<>(Map.of("order",orderId,"command",c)));var prior=store.jdbc().queryForList("SELECT id,request_fingerprint FROM lin_mail_message WHERE requested_by=? AND request_key=?",actor,c.requestKey());
  if(!prior.isEmpty()){if(!hash.equals(prior.get(0).get("request_fingerprint")))throw error(409,"请求号已用于其他通知");return ((Number)prior.get(0).get("id")).longValue();}
  var order=order(orderId,true);var contacts=contacts(order,true);if(!version(order,contacts).equals(c.expectedToken()))throw error(409,"送检单或联系人已变化，请重新打开核对");
  var contact=contacts.stream().filter(r->((Number)r.get("InfoHospitalContactId")).longValue()==c.contactId()).findFirst().orElseThrow(()->error(404,"当前医院联系人不存在"));
  String mail=Objects.toString(contact.get("Mail"),"");if(mail.isBlank())throw error(400,"联系人邮箱为空");
  if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_mail_message WHERE business_type='SAMPLE_NOTICE' AND business_id=? AND status IN ('READY','SENDING','UNKNOWN','ACCEPTED'))",Boolean.class,String.valueOf(orderId))))throw error(409,"当前送检单存在未完成投递，请先处理投递记录，勿重复创建");
  var profile=store.one("SELECT UserName,BCCMail FROM lin_lb_dictmailserver WHERE DictMailServerId=? FOR SHARE",profileId);
  var routing=LegacyMailRoutingRules.route(LegacyMailRoutingRules.Kind.SAMPLE_NOTICE,mail,overrideTo,Objects.toString(profile.get("BCCMail"),""));String sender=Objects.toString(profile.get("UserName"),"");
  long id=store.insert("INSERT INTO lin_mail_message(business_type,business_id,requested_by,request_key,request_fingerprint,smtp_profile_id,sender_address,sender_name,recipients_json,bcc_json,subject,body_html,attachments_json,source_version,callback_json) VALUES('SAMPLE_NOTICE',?,?,?,?,?,?,?,?,?,?,?,'[]',?,?)",String.valueOf(orderId),actor,c.requestKey(),hash,profileId,sender,LegacyMailAddressRules.senderName(sender),store.encode(Map.of("raw",routing.to())),store.encode(Map.of("raw",routing.bcc())),c.title(),c.html(),c.expectedToken(),store.encode(Map.of("orderId",orderId,"hospitalId",order.get("InfoHospitalId"))));
  audit(actor,"SAMPLE_NOTICE_QUEUED",id,c.requestKey());return id;
 }));}
 private void audit(int actor,String action,long id,String request){store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,?,'LEGACY_SAMPLE_NOTICE',?,?,?)",actor,action,String.valueOf(id),request,store.encode(Map.of("messageId",id)));}
 private Map<String,Object> rawMessage(long id,boolean lock){if(id<1)throw error(400,"通知编号无效");return store.one("SELECT * FROM lin_mail_message WHERE id=? AND business_type='SAMPLE_NOTICE'"+(lock?" FOR UPDATE":""),id);}
 private Map<String,Object> latestAttempt(long id,boolean lock){var rows=store.jdbc().queryForList("SELECT * FROM lin_mail_attempt WHERE message_id=? ORDER BY attempt_no DESC LIMIT 1"+(lock?" FOR UPDATE":""),id);return rows.isEmpty()?Map.of():rows.get(0);}
 private String reconcileToken(Map<String,Object> message,Map<String,Object> attempt){return fingerprint(new TreeMap<>(Map.of("message",new TreeMap<>(message),"attempt",new TreeMap<>(attempt))));}
 public Map<String,Object> message(long id){read();var raw=rawMessage(id,false);var result=new LinkedHashMap<String,Object>();for(String field:List.of("id","business_id","status","callback_status","attempt_count","last_error_code","last_error_message","accepted_at","created_at","subject","sender_address","recipients_json","bcc_json"))result.put(field,raw.get(field));result.put("editToken",reconcileToken(raw,latestAttempt(id,false)));result.put("attempts",store.jdbc().queryForList("SELECT id,attempt_no,status,started_at,finished_at,smtp_message_id,resolved_by,resolved_at,resolution_note FROM lin_mail_attempt WHERE message_id=? ORDER BY attempt_no DESC LIMIT 50",id));return result;}
 public Map<String,Object> messages(long orderId){read();order(orderId,false);return Map.of("items",store.jdbc().queryForList("SELECT id,status,callback_status,attempt_count,last_error_code,last_error_message,created_at FROM lin_mail_message WHERE business_type='SAMPLE_NOTICE' AND business_id=? ORDER BY id DESC LIMIT 50",String.valueOf(orderId)));}
 public Map<String,Object> cancel(long id){tx.executeWithoutResult(status->{if(!policy(true)[1])throw error(409,"通知维护尚未开启");var row=rawMessage(id,true);if("CANCELLED".equals(row.get("status")))return;if(!"READY".equals(row.get("status")))throw error(409,"仅待发送通知可以取消，已开始投递的邮件不能撤回");store.jdbc().update("UPDATE lin_mail_message SET status='CANCELLED',callback_status='CANCELLED' WHERE id=?",id);audit(access.current().userId(),"SAMPLE_NOTICE_CANCELLED",id,row.get("request_key").toString());});return message(id);}
 public Map<String,Object> reconcile(long id,ReconcileCommand c){
  tx.executeWithoutResult(status->{
   if(!policy(true)[3])throw error(409,"投递核对尚未开启");
   if(c==null||c.resolution()==null||!Set.of("ACCEPTED","NOT_ACCEPTED").contains(c.resolution())||!c.evidenceConfirmed()||c.note()==null||c.note().isBlank()||c.note().length()<10||c.note().length()>1000||c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}")||c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}"))throw error(400,"请确认已核对SMTP日志，并填写10至1000字依据");
   int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);String hash=fingerprint(new TreeMap<>(Map.of("messageId",id,"command",c)));
   var replay=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND action='SAMPLE_NOTICE_RECONCILED' AND resource_type='LEGACY_SAMPLE_NOTICE' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
   if(!replay.isEmpty()){if(!hash.equals(json(replay.get(0).get("after_json")).path("requestFingerprint").asText()))throw error(409,"请求号已用于其他核对操作");return;}
   var row=rawMessage(id,true);var attempt=latestAttempt(id,true);String current=row.get("status").toString();
   if(!Set.of("UNKNOWN","SENDING").contains(current)||attempt.isEmpty()||!current.equals(attempt.get("status"))||((Number)attempt.get("attempt_no")).intValue()!=((Number)row.get("attempt_count")).intValue())throw error(409,"当前记录不需要核对或投递状态不一致");
   if(!reconcileToken(row,attempt).equals(c.expectedToken()))throw error(409,"投递记录已变化，请重新打开核对");
   if(current.equals("SENDING")&&!c.workerStoppedConfirmed())throw error(409,"请先停止原发送进程并明确确认，再处理投递中记录");
   boolean accepted=c.resolution().equals("ACCEPTED");
   store.jdbc().update("UPDATE lin_mail_attempt SET status=?,finished_at=COALESCE(finished_at,NOW()),resolved_by=?,resolved_at=NOW(),resolution_note=? WHERE id=?",accepted?"CONFIRMED_ACCEPTED":"CONFIRMED_NOT_ACCEPTED",actor,c.note(),attempt.get("id"));
   store.jdbc().update("UPDATE lin_mail_message SET status=?,callback_status='PENDING',accepted_at=IF(?=1,COALESCE(accepted_at,NOW()),NULL),last_error_code=?,last_error_message=? WHERE id=?",accepted?"ACCEPTED":"READY",accepted?1:0,accepted?"MANUAL_ACCEPTED":"MANUAL_NOT_ACCEPTED",accepted?"已核对SMTP接受，等待业务状态回写":"已核对未接受，须明确点击发送后才重试",id);
   store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'SAMPLE_NOTICE_RECONCILED','LEGACY_SAMPLE_NOTICE',?,?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(Map.of("messageStatus",current,"attemptId",attempt.get("id"),"attemptStatus",attempt.get("status"))),store.encode(Map.of("messageId",id,"requestFingerprint",hash,"resolution",c.resolution(),"note",c.note(),"workerStoppedConfirmed",c.workerStoppedConfirmed(),"evidenceConfirmed",true)));
  });completeCallback(id);return message(id);
 }
 private record Claim(Map<String,Object> message,LegacyMailTransport.Profile profile,String claimToken,String smtpMessageId){}
 public Map<String,Object> dispatch(long id){
  if(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw error(409,"邮件投递必须在独立事务之外执行");
  Claim claim=tx.execute(status->{
   var p=policy(true);if(!p[1])throw error(409,"通知处理尚未开启");var row=rawMessage(id,true);String current=row.get("status").toString();
   if(current.equals("SENT")||current.equals("ACCEPTED"))return new Claim(row,null,null,null);
   if(!p[2]||!transport.available())throw error(409,"邮件发送尚未开启");if(!current.equals("READY"))throw error(409,"当前投递不能重试；结果未确认时请先核对");
   long orderId=Long.parseLong(row.get("business_id").toString());var order=order(orderId,true);
   if(!version(order,contacts(order,true)).equals(row.get("source_version"))){store.jdbc().update("UPDATE lin_mail_message SET status='BLOCKED',last_error_code='SOURCE_CHANGED',last_error_message='送检单或联系人已变化，请重新核对正文' WHERE id=?",id);return new Claim(row,null,null,null);}
   Boolean recent=store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_mail_message WHERE business_type='SAMPLE_NOTICE' AND accepted_at>DATE_SUB(NOW(),INTERVAL 5 SECOND))",Boolean.class);if(Boolean.TRUE.equals(recent))throw error(409,"发送频繁，请稍后尝试");
   var smtp=store.one("SELECT ServerHost,Port,UserName,Psw FROM lin_lb_dictmailserver WHERE DictMailServerId=? FOR SHARE",row.get("smtp_profile_id"));if(!Objects.equals(smtp.get("UserName"),row.get("sender_address")))throw error(409,"发件人配置已变化，请重新创建通知");
   int attempt=((Number)row.get("attempt_count")).intValue()+1;String token=UUID.randomUUID().toString();
   String smtpMessageId="<petlis-notice-"+id+"-"+attempt+"-"+token+"@petlis.invalid>";store.insert("INSERT INTO lin_mail_attempt(message_id,attempt_no,claim_token,status,smtp_message_id) VALUES(?,?,?,'SENDING',?)",id,attempt,token,smtpMessageId);store.jdbc().update("UPDATE lin_mail_message SET status='SENDING',attempt_count=? WHERE id=?",attempt,id);
   return new Claim(row,new LegacyMailTransport.Profile(smtp.get("ServerHost").toString(),((Number)smtp.get("Port")).intValue(),smtp.get("UserName").toString(),smtp.get("Psw").toString(),jsonRaw(row.get("bcc_json"))),token,smtpMessageId);
  });
  if(claim==null)throw error(409,"未取得邮件投递任务");
  if(claim.profile()!=null){
   String outcome="ACCEPTED",code="",description="";
   try{transport.send(claim.profile(),new LegacyMailTransport.Message(jsonRaw(claim.message().get("recipients_json")),null,claim.message().get("subject").toString(),claim.message().get("body_html").toString(),List.of(),claim.smtpMessageId()),LegacyMailRoutingRules.Kind.SAMPLE_NOTICE);}
   catch(IllegalArgumentException e){outcome="FAILED";code="INVALID_MESSAGE";description="邮件地址或内容无效，未完成发送，请重新编辑";}
   catch(LegacyMailTransport.DeliveryUncertainException e){outcome="UNKNOWN";code="SMTP_UNCERTAIN";description="SMTP结果未确认，禁止直接重复发送，请先核对";}
   final String result=outcome,errorCode=code,errorMessage=description;
   tx.executeWithoutResult(status->{var row=rawMessage(id,true);if(!"SENDING".equals(row.get("status")))throw error(409,"投递状态已变化");int changed=store.jdbc().update("UPDATE lin_mail_attempt SET status=?,finished_at=NOW(),error_code=?,error_message=? WHERE message_id=? AND claim_token=? AND status='SENDING'",result,errorCode,errorMessage,id,claim.claimToken());if(changed!=1)throw error(409,"投递领取记录不一致");store.jdbc().update("UPDATE lin_mail_message SET status=?,accepted_at=IF(?='ACCEPTED',NOW(),NULL),last_error_code=?,last_error_message=? WHERE id=?",result,result,errorCode,errorMessage,id);});
  }
  completeCallback(id);return message(id);
 }
 private void completeCallback(long id){
  try{tx.executeWithoutResult(status->{if(!policy(true)[1])return;var row=rawMessage(id,true);if(!"ACCEPTED".equals(row.get("status")))return;
   var callback=json(row.get("callback_json"));long orderId=callback.path("orderId").asLong(),hospital=callback.path("hospitalId").asLong();var order=order(orderId,true);if(((Number)order.get("InfoHospitalId")).longValue()!=hospital)throw error(409,"送检单医院已变化，已接受邮件需人工核对回写");
   store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET IsSendNoticeMail=1 WHERE ListDiagnosisProjectId=?",orderId);store.jdbc().update("UPDATE lin_mail_message SET status='SENT',callback_status='DONE',last_error_code='',last_error_message='' WHERE id=?",id);audit(access.current().userId(),"SAMPLE_NOTICE_COMPLETED",id,row.get("request_key").toString());
  });}catch(Exception e){tx.executeWithoutResult(status->store.jdbc().update("UPDATE lin_mail_message SET callback_status='FAILED',last_error_code='CALLBACK_PENDING',last_error_message='邮件已接受，业务回写待恢复；不能再次发送' WHERE id=? AND status='ACCEPTED'",id));}
 }
 private com.fasterxml.jackson.databind.JsonNode json(Object value){try{return new ObjectMapper().readTree(value.toString());}catch(Exception e){throw error(409,"邮件快照无效");}}
 private String jsonRaw(Object json){try{return new ObjectMapper().readTree(json.toString()).path("raw").asText();}catch(Exception e){throw error(409,"邮件收件快照无效");}}
 private static BusinessException error(int code,String message){return new BusinessException(code,message);}
}
