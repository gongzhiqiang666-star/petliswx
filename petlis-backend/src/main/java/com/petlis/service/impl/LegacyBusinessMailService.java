package com.petlis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Shared durable delivery for legacy business emails; adapters retain source-specific callbacks.
 * Every network call is outside DB transactions. There is no background worker.
 */
public class LegacyBusinessMailService {
 public enum Kind { SAMPLE_NOTICE, REPORT_MAIL, FINANCE_MAIL }
 private final Kind kind;private final LegacyReportFiles files;
 protected final FeatureStore store;protected final AccessService access;private final LegacyOrderDetailService renderer;
 protected final LegacyMailTransport transport;protected final TransactionTemplate tx;protected final int profileId;
 protected final String overrideTo;private final byte[] signingKey;
 protected LegacyBusinessMailService(FeatureStore store,AccessService access,LegacyOrderDetailService renderer,LegacyMailTransport transport,PlatformTransactionManager manager,int profileId,String overrideTo,String secret,Kind kind,LegacyReportFiles files){this.kind=kind;this.files=files;this.store=store;this.access=access;this.renderer=renderer;this.transport=transport;this.tx=new TransactionTemplate(manager);this.profileId=profileId;this.overrideTo=overrideTo;this.signingKey=secret.getBytes(StandardCharsets.UTF_8);}
 public record Command(long contactId,String title,String html,String requestKey,String expectedToken,String pdfTitle){public Command(long contactId,String title,String html,String requestKey,String expectedToken){this(contactId,title,html,requestKey,expectedToken,null);}}
 public record ReconcileCommand(String resolution,String note,boolean evidenceConfirmed,boolean workerStoppedConfirmed,String expectedToken,String requestKey){}
 protected boolean[] policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='"+configKey()+"' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));
  if(rows.size()!=1)return new boolean[]{false,false,false,false};
  try{var j=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=flag(j,"enabled"),write=read&&flag(j,"writeEnabled");return new boolean[]{read,write,write&&flag(j,"sendEnabled"),write&&flag(j,"reconcileEnabled")};}catch(Exception e){throw error(409,"业务邮件参数无效");}
 }
 private boolean flag(com.fasterxml.jackson.databind.JsonNode node,String key){return node.path(key).isBoolean()&&node.path(key).asBoolean();}
 private String configKey(){return switch(kind){case SAMPLE_NOTICE->"legacy-sample-notice";case REPORT_MAIL->"legacy-report-mail";case FINANCE_MAIL->"legacy-finance-mail";};}
 private String prefix(){return switch(kind){case SAMPLE_NOTICE->"sampleNotice";case REPORT_MAIL->"reportMail";case FINANCE_MAIL->"financeMail";};}
 private String resourceType(){return "LEGACY_"+kind.name();}
 private String action(String suffix){return kind.name()+"_"+suffix;}
 private LegacyMailRoutingRules.Kind routingKind(){return switch(kind){case SAMPLE_NOTICE->LegacyMailRoutingRules.Kind.SAMPLE_NOTICE;case REPORT_MAIL->LegacyMailRoutingRules.Kind.REPORT;case FINANCE_MAIL->LegacyMailRoutingRules.Kind.FINANCE;};}
 public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of(prefix(),p[0],prefix()+"Writes",p[1],prefix()+"Send",p[2]&&transport.available(),prefix()+"Reconcile",p[3]);}
 protected void read(){if(!policy(false)[0])throw error(409,"业务邮件尚未开启");}
 private Map<String,Object> order(long id,boolean lock){if(id<1||id>Integer.MAX_VALUE)throw error(400,"送检单编号无效");return store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?"+(lock?" FOR UPDATE":""),id);}
 private List<Map<String,Object>> contacts(Map<String,Object> order,boolean lock){return store.jdbc().queryForList("SELECT InfoHospitalContactId,Name,Mail FROM lin_lb_infohospitalcontact WHERE InfoHospitalId=? ORDER BY InfoHospitalContactId"+(lock?" FOR UPDATE":""),order.get("InfoHospitalId"));}
 protected String fingerprint(Object value){try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(signingKey,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(((kind==Kind.SAMPLE_NOTICE?"sample-notice-v1:":kind==Kind.REPORT_MAIL?"report-mail-v1:":"finance-mail-v1:")+store.encode(value)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("业务邮件校验失败");}}
 private record Source(long orderId,Map<String,Object> order,Map<String,Object> detail){}
 private Source source(long id,boolean lock){if(kind==Kind.SAMPLE_NOTICE)return new Source(id,order(id,lock),Map.of());var peek=store.one("SELECT ListDiagnosisProjectId FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=?",id);long parent=((Number)peek.get("ListDiagnosisProjectId")).longValue();var order=order(parent,lock);var detail=store.one("SELECT d.*,i.MailReport,i.ReportResult FROM lin_lb_diagnosisdetail d LEFT JOIN lin_lb_diagnosisdetail_info i ON i.diagnosisdetail_id=d.DiagnosisDetailId WHERE d.DiagnosisDetailId=? AND d.ListDiagnosisProjectId=?"+(lock?" FOR UPDATE":""),id,parent);return new Source(parent,order,detail);}
 private String version(Source source,List<Map<String,Object>> contacts){var payload=new TreeMap<String,Object>();payload.put("order",new TreeMap<>(source.order()));payload.put("contacts",contacts);if(kind==Kind.REPORT_MAIL)payload.put("detail",new TreeMap<>(source.detail()));return fingerprint(payload);}
 private Map<String,Object> commandPayload(Command c){var result=new LinkedHashMap<String,Object>();result.put("contactId",c.contactId());result.put("title",c.title());result.put("html",c.html());result.put("requestKey",c.requestKey());result.put("expectedToken",c.expectedToken());if(c.pdfTitle()!=null)result.put("pdfTitle",c.pdfTitle());return result;}
 private String reportPath(Source source,String supplied){var project=store.one("SELECT AnimalName,MasterName,UserNO,ReportNO,DiagnosticResults FROM vwlistdiagnosisproject WHERE ListDiagnosisProjectId=?",source.orderId());String name=LegacyReportFiles.filename(project,source.detail(),supplied,java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")));return files.path(source.orderId(),((Number)source.detail().get("DiagnosisDetailId")).longValue(),name);}
 public byte[] pdf(long id,String title){read();if(kind!=Kind.REPORT_MAIL)throw error(400,"当前业务没有PDF");return files.read(reportPath(source(id,false),title));}

 public Map<String,Object> options(long id,int page,int size){read();var source=source(id,false);if(page<0||size<1||size>100)throw error(400,"分页无效");String table=kind==Kind.SAMPLE_NOTICE?"lin_lb_dictmailreportnotice":"lin_lb_dictmaildiagnosisreport",columns=kind==Kind.SAMPLE_NOTICE?"id,tplname,is_active":"id,model_type AS tplname";var templates=store.page("SELECT COUNT(*) FROM "+table,"SELECT "+columns+" FROM "+table+" ORDER BY id",page,size);@SuppressWarnings("unchecked")var items=(List<Map<String,Object>>)templates.get("items");for(var item:items)item.put("tplname",LegacyDictionarySelector.display(item.get("tplname")));return Map.of("contacts",contacts(source.order(),false),"templates",templates);}
 public Map<String,Object> draft(long id,long template){read();var source=source(id,false);var result=new LinkedHashMap<String,Object>();result.put("orderId",source.orderId());result.put("hospitalId",source.order().get("InfoHospitalId"));result.put("editToken",version(source,contacts(source.order(),false)));
  if(kind==Kind.SAMPLE_NOTICE){var body=store.one("SELECT tpltext FROM lin_lb_dictmailreportnotice WHERE id=?",template);result.put("title","样本收到通知");result.put("html",renderer.renderSampleNoticeBody(id,Objects.toString(body.get("tpltext"),"")));}
  else{result.put("title",Objects.toString(source.detail().get("MailTitle"),""));String html=Objects.toString(source.detail().get("MailReport"),"");if(template>0){var body=store.one("SELECT model_content FROM lin_lb_dictmaildiagnosisreport WHERE id=?",template);html=renderer.renderReportMailBody(source.orderId(),id,Objects.toString(body.get("model_content"),""));}result.put("html",html);String path=reportPath(source,null);result.put("pdfTitle",path.substring(path.lastIndexOf('/')+1));result.put("pdfPath",path);try{files.inspect(path);result.put("pdfAvailable",true);}catch(BusinessException e){result.put("pdfAvailable",false);}}
  return result;
 }
 private void validate(Command c){
  if(c==null||c.contactId()<1||c.title()==null||kind==Kind.SAMPLE_NOTICE&&c.title().isBlank()||c.title().length()>2000||c.title().indexOf('\r')>=0||c.title().indexOf('\n')>=0||c.title().indexOf('\0')>=0||c.html()==null||kind==Kind.SAMPLE_NOTICE&&c.html().isBlank()||c.html().getBytes(StandardCharsets.UTF_8).length>2*1024*1024)throw error(400,"联系人、标题或正文无效");
  if(c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}")||c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}"))throw error(400,"请重新打开业务邮件后提交");
 }
 public long queue(long orderId,Command c){return Objects.requireNonNull(tx.execute(status->{
  if(!policy(true)[1])throw error(409,"业务邮件创建尚未开启");validate(c);int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String hash=fingerprint(new TreeMap<>(Map.of("order",orderId,"command",commandPayload(c))));var prior=store.jdbc().queryForList("SELECT id,business_type,request_fingerprint FROM lin_mail_message WHERE requested_by=? AND request_key=?",actor,c.requestKey());
  if(!prior.isEmpty()){if(!kind.name().equals(prior.get(0).get("business_type"))||!hash.equals(prior.get(0).get("request_fingerprint")))throw error(409,"请求号已用于其他通知");return ((Number)prior.get(0).get("id")).longValue();}
  var source=source(orderId,true);var order=source.order();var contacts=contacts(order,true);if(!version(source,contacts).equals(c.expectedToken()))throw error(409,"送检单或联系人已变化，请重新打开核对");
  var contact=contacts.stream().filter(r->((Number)r.get("InfoHospitalContactId")).longValue()==c.contactId()).findFirst().orElseThrow(()->error(404,"当前医院联系人不存在"));
  String mail=Objects.toString(contact.get("Mail"),"");if(mail.isBlank())throw error(400,"联系人邮箱为空");
  if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_mail_message WHERE business_type='"+kind.name()+"' AND business_id=? AND status IN ('READY','SENDING','UNKNOWN','ACCEPTED'))",Boolean.class,String.valueOf(orderId))))throw error(409,"当前送检单存在未完成投递，请先处理投递记录，勿重复创建");
  var profile=store.one("SELECT UserName,BCCMail FROM lin_lb_dictmailserver WHERE DictMailServerId=? FOR SHARE",profileId);
  var routing=LegacyMailRoutingRules.route(routingKind(),mail,overrideTo,Objects.toString(profile.get("BCCMail"),""));String sender=Objects.toString(profile.get("UserName"),"");
  String artifacts=kind==Kind.SAMPLE_NOTICE?"[]":store.encode(Map.of("requiredPdf",files.inspect(reportPath(source,c.pdfTitle())),"sendAttachments",false));
  long id=store.insert("INSERT INTO lin_mail_message(business_type,business_id,requested_by,request_key,request_fingerprint,smtp_profile_id,sender_address,sender_name,recipients_json,bcc_json,subject,body_html,attachments_json,source_version,callback_json) VALUES('"+kind.name()+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?)",String.valueOf(orderId),actor,c.requestKey(),hash,profileId,sender,LegacyMailAddressRules.senderName(sender),store.encode(Map.of("raw",routing.to())),store.encode(Map.of("raw",routing.bcc())),c.title(),c.html(),artifacts,c.expectedToken(),store.encode(Map.of("orderId",source.orderId(),"hospitalId",order.get("InfoHospitalId"),"detailId",kind==Kind.SAMPLE_NOTICE?0:orderId)));
  audit(actor,action("QUEUED"),id,c.requestKey());return id;
 }));}
 protected void audit(int actor,String action,long id,String request){store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,?,'"+resourceType()+"',?,?,?)",actor,action,String.valueOf(id),request,store.encode(Map.of("messageId",id)));}
 private Map<String,Object> rawMessage(long id,boolean lock){if(id<1)throw error(400,"通知编号无效");return store.one("SELECT * FROM lin_mail_message WHERE id=? AND business_type='"+kind.name()+"'"+(lock?" FOR UPDATE":""),id);}
 private Map<String,Object> latestAttempt(long id,boolean lock){var rows=store.jdbc().queryForList("SELECT * FROM lin_mail_attempt WHERE message_id=? ORDER BY attempt_no DESC LIMIT 1"+(lock?" FOR UPDATE":""),id);return rows.isEmpty()?Map.of():rows.get(0);}
 private String reconcileToken(Map<String,Object> message,Map<String,Object> attempt){return fingerprint(new TreeMap<>(Map.of("message",new TreeMap<>(message),"attempt",new TreeMap<>(attempt))));}
 public Map<String,Object> message(long id){read();var raw=rawMessage(id,false);var result=new LinkedHashMap<String,Object>();for(String field:List.of("id","business_id","status","callback_status","attempt_count","last_error_code","last_error_message","accepted_at","created_at","subject","sender_address","recipients_json","bcc_json"))result.put(field,raw.get(field));result.put("editToken",reconcileToken(raw,latestAttempt(id,false)));result.put("attempts",store.jdbc().queryForList("SELECT id,attempt_no,status,started_at,finished_at,smtp_message_id,resolved_by,resolved_at,resolution_note FROM lin_mail_attempt WHERE message_id=? ORDER BY attempt_no DESC LIMIT 50",id));return result;}
 public Map<String,Object> messages(long orderId){read();source(orderId,false);return Map.of("items",store.jdbc().queryForList("SELECT id,status,callback_status,attempt_count,last_error_code,last_error_message,created_at FROM lin_mail_message WHERE business_type='"+kind.name()+"' AND business_id=? ORDER BY id DESC LIMIT 50",String.valueOf(orderId)));}
 public Map<String,Object> cancel(long id){tx.executeWithoutResult(status->{if(!policy(true)[1])throw error(409,"通知维护尚未开启");var row=rawMessage(id,true);if("CANCELLED".equals(row.get("status")))return;if(!"READY".equals(row.get("status")))throw error(409,"仅待发送通知可以取消，已开始投递的邮件不能撤回");store.jdbc().update("UPDATE lin_mail_message SET status='CANCELLED',callback_status='CANCELLED' WHERE id=?",id);audit(access.current().userId(),action("CANCELLED"),id,row.get("request_key").toString());});return message(id);}
 public Map<String,Object> reconcile(long id,ReconcileCommand c){
  tx.executeWithoutResult(status->{
   if(!policy(true)[3])throw error(409,"投递核对尚未开启");
   if(c==null||c.resolution()==null||!Set.of("ACCEPTED","NOT_ACCEPTED").contains(c.resolution())||!c.evidenceConfirmed()||c.note()==null||c.note().isBlank()||c.note().length()<10||c.note().length()>1000||c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}")||c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}"))throw error(400,"请确认已核对SMTP日志，并填写10至1000字依据");
   int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);String hash=fingerprint(new TreeMap<>(Map.of("messageId",id,"command",c)));
   var replay=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND action='"+action("RECONCILED")+"' AND resource_type='"+resourceType()+"' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
   if(!replay.isEmpty()){if(!hash.equals(json(replay.get(0).get("after_json")).path("requestFingerprint").asText()))throw error(409,"请求号已用于其他核对操作");return;}
   var row=rawMessage(id,true);var attempt=latestAttempt(id,true);String current=row.get("status").toString();
   if(!Set.of("UNKNOWN","SENDING").contains(current)||attempt.isEmpty()||!current.equals(attempt.get("status"))||((Number)attempt.get("attempt_no")).intValue()!=((Number)row.get("attempt_count")).intValue())throw error(409,"当前记录不需要核对或投递状态不一致");
   if(!reconcileToken(row,attempt).equals(c.expectedToken()))throw error(409,"投递记录已变化，请重新打开核对");
   if(current.equals("SENDING")&&!c.workerStoppedConfirmed())throw error(409,"请先停止原发送进程并明确确认，再处理投递中记录");
   boolean accepted=c.resolution().equals("ACCEPTED");
   store.jdbc().update("UPDATE lin_mail_attempt SET status=?,finished_at=COALESCE(finished_at,NOW()),resolved_by=?,resolved_at=NOW(),resolution_note=? WHERE id=?",accepted?"CONFIRMED_ACCEPTED":"CONFIRMED_NOT_ACCEPTED",actor,c.note(),attempt.get("id"));
   store.jdbc().update("UPDATE lin_mail_message SET status=?,callback_status='PENDING',accepted_at=IF(?=1,COALESCE(accepted_at,NOW()),NULL),last_error_code=?,last_error_message=? WHERE id=?",accepted?"ACCEPTED":"READY",accepted?1:0,accepted?"MANUAL_ACCEPTED":"MANUAL_NOT_ACCEPTED",accepted?"已核对SMTP接受，等待业务状态回写":"已核对未接受，须明确点击发送后才重试",id);
   store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'"+action("RECONCILED")+"','"+resourceType()+"',?,?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(Map.of("messageStatus",current,"attemptId",attempt.get("id"),"attemptStatus",attempt.get("status"))),store.encode(Map.of("messageId",id,"requestFingerprint",hash,"resolution",c.resolution(),"note",c.note(),"workerStoppedConfirmed",c.workerStoppedConfirmed(),"evidenceConfirmed",true)));
  });completeCallback(id);return message(id);
 }
 private record Claim(Map<String,Object> message,LegacyMailTransport.Profile profile,String claimToken,String smtpMessageId,List<LegacyMailTransport.Attachment> attachments){Claim(Map<String,Object> message,LegacyMailTransport.Profile profile,String token,String messageId){this(message,profile,token,messageId,List.of());}}
 protected boolean sourceUnchanged(Map<String,Object> row){long id=Long.parseLong(row.get("business_id").toString());var source=source(id,true);return version(source,contacts(source.order(),true)).equals(row.get("source_version"));}
 protected List<LegacyMailTransport.Attachment> deliveryAttachments(Map<String,Object> row){if(kind==Kind.REPORT_MAIL){var expected=json(row.get("attachments_json")).path("requiredPdf");var actual=files.inspect(expected.path("path").asText());if(!expected.path("sha256").asText().equals(actual.get("sha256")))throw error(409,"PDF已变更");}return List.of();}
 protected void businessCallback(Map<String,Object> row){
  var callback=json(row.get("callback_json"));long orderId=callback.path("orderId").asLong(),hospital=callback.path("hospitalId").asLong();var order=order(orderId,true);if(((Number)order.get("InfoHospitalId")).longValue()!=hospital)throw error(409,"送检单医院已变化，已接受邮件需人工核对回写");
  if(kind==Kind.SAMPLE_NOTICE)store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET IsSendNoticeMail=1 WHERE ListDiagnosisProjectId=?",orderId);
  else{long detail=callback.path("detailId").asLong();store.one("SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=? AND ListDiagnosisProjectId=? FOR UPDATE",detail,orderId);var day=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).atStartOfDay();store.jdbc().update("UPDATE lin_lb_diagnosisdetail SET IsSendMail=1,SendMailDt=? WHERE DiagnosisDetailId=?",day,detail);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SendReportDt=?,SampleStatus=3 WHERE ListDiagnosisProjectId=?",day,orderId);}
 }

 public Map<String,Object> dispatch(long id){
  if(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw error(409,"邮件投递必须在独立事务之外执行");
  Claim claim=tx.execute(status->{
   var p=policy(true);if(!p[1])throw error(409,"通知处理尚未开启");var row=rawMessage(id,true);String current=row.get("status").toString();
   if(current.equals("SENT")||current.equals("ACCEPTED"))return new Claim(row,null,null,null);
   if(!p[2]||!transport.available())throw error(409,"邮件发送尚未开启");if(!current.equals("READY"))throw error(409,"当前投递不能重试；结果未确认时请先核对");
   if(!sourceUnchanged(row)){store.jdbc().update("UPDATE lin_mail_message SET status='BLOCKED',last_error_code='SOURCE_CHANGED',last_error_message='业务资料或联系人已变化，请重新核对正文' WHERE id=?",id);return new Claim(row,null,null,null);}
   List<LegacyMailTransport.Attachment> attachments;try{attachments=deliveryAttachments(row);}catch(BusinessException e){store.jdbc().update("UPDATE lin_mail_message SET status='BLOCKED',last_error_code='PDF_CHANGED',last_error_message='报告PDF缺失或已变更，请重新核对' WHERE id=?",id);return new Claim(row,null,null,null);}
   Boolean recent=store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_mail_message WHERE business_type='"+kind.name()+"' AND accepted_at>DATE_SUB(NOW(),INTERVAL 5 SECOND))",Boolean.class);if(kind!=Kind.FINANCE_MAIL&&Boolean.TRUE.equals(recent))throw error(409,"发送频繁，请稍后尝试");
   var smtp=store.one("SELECT ServerHost,Port,UserName,Psw FROM lin_lb_dictmailserver WHERE DictMailServerId=? FOR SHARE",row.get("smtp_profile_id"));if(!Objects.equals(smtp.get("UserName"),row.get("sender_address")))throw error(409,"发件人配置已变化，请重新创建通知");
   int attempt=((Number)row.get("attempt_count")).intValue()+1;String token=UUID.randomUUID().toString();
   String smtpMessageId="<petlis-"+(kind==Kind.SAMPLE_NOTICE?"notice":kind==Kind.REPORT_MAIL?"report":"finance")+"-"+id+"-"+attempt+"-"+token+"@petlis.invalid>";store.insert("INSERT INTO lin_mail_attempt(message_id,attempt_no,claim_token,status,smtp_message_id) VALUES(?,?,?,'SENDING',?)",id,attempt,token,smtpMessageId);store.jdbc().update("UPDATE lin_mail_message SET status='SENDING',attempt_count=? WHERE id=?",attempt,id);
   return new Claim(row,new LegacyMailTransport.Profile(smtp.get("ServerHost").toString(),((Number)smtp.get("Port")).intValue(),smtp.get("UserName").toString(),smtp.get("Psw").toString(),jsonRaw(row.get("bcc_json"))),token,smtpMessageId,attachments);
  });
  if(claim==null)throw error(409,"未取得邮件投递任务");
  if(claim.profile()!=null){
   String outcome="ACCEPTED",code="",description="";
   try{transport.send(claim.profile(),new LegacyMailTransport.Message(jsonRaw(claim.message().get("recipients_json")),null,claim.message().get("subject").toString(),claim.message().get("body_html").toString(),claim.attachments(),claim.smtpMessageId()),routingKind());}
   catch(IllegalArgumentException e){outcome="FAILED";code="INVALID_MESSAGE";description="邮件地址或内容无效，未完成发送，请重新编辑";}
   catch(LegacyMailTransport.DeliveryUncertainException e){outcome="UNKNOWN";code="SMTP_UNCERTAIN";description="SMTP结果未确认，禁止直接重复发送，请先核对";}
   final String result=outcome,errorCode=code,errorMessage=description;
   tx.executeWithoutResult(status->{var row=rawMessage(id,true);if(!"SENDING".equals(row.get("status")))throw error(409,"投递状态已变化");int changed=store.jdbc().update("UPDATE lin_mail_attempt SET status=?,finished_at=NOW(),error_code=?,error_message=? WHERE message_id=? AND claim_token=? AND status='SENDING'",result,errorCode,errorMessage,id,claim.claimToken());if(changed!=1)throw error(409,"投递领取记录不一致");store.jdbc().update("UPDATE lin_mail_message SET status=?,accepted_at=IF(?='ACCEPTED',NOW(),NULL),last_error_code=?,last_error_message=? WHERE id=?",result,result,errorCode,errorMessage,id);});
  }
  completeCallback(id);return message(id);
 }
 private void completeCallback(long id){
  try{tx.executeWithoutResult(status->{if(!policy(true)[1])return;var row=rawMessage(id,true);if(!"ACCEPTED".equals(row.get("status")))return;
   businessCallback(row);
store.jdbc().update("UPDATE lin_mail_message SET status='SENT',callback_status='DONE',last_error_code='',last_error_message='' WHERE id=?",id);audit(access.current().userId(),action("COMPLETED"),id,row.get("request_key").toString());
  });}catch(Exception e){tx.executeWithoutResult(status->store.jdbc().update("UPDATE lin_mail_message SET callback_status='FAILED',last_error_code='CALLBACK_PENDING',last_error_message='邮件已接受，业务回写待恢复；不能再次发送' WHERE id=? AND status='ACCEPTED'",id));}
 }
 protected com.fasterxml.jackson.databind.JsonNode json(Object value){try{return new ObjectMapper().readTree(value.toString());}catch(Exception e){throw error(409,"邮件快照无效");}}
 private String jsonRaw(Object json){try{return new ObjectMapper().readTree(json.toString()).path("raw").asText();}catch(Exception e){throw error(409,"邮件收件快照无效");}}
 private static BusinessException error(int code,String message){return new BusinessException(code,message);}
}
