package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import com.petlis.common.security.ReviewPermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.time.*;

/** Durable in-app inbox and transactional push outbox; independent of legacy trigger wording. */
@Service @RequiredArgsConstructor
public class BusinessNotifications {
 private final FeatureStore store;
 private final SubscriptionPush push;
 private final ReviewPermission permission;
 private final TransactionTemplate transactions;
 private final Environment env;
 private final ObjectMapper json;
 @Transactional public void emit(int userId,String type,String key,String title,String message,String page){
  if(userId<=0 || key.length()>100 || title.length()>200 || message.length()>1000 || page.length()>300)throw new IllegalArgumentException("Invalid notification metadata");
  store.jdbc().update("INSERT INTO lin_notification_event(user_id,event_type,business_key,title,message,page_path) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id=id",userId,type,key,title,message,page);
 }
 @Transactional public void emitForOrder(int orderId,Integer userId,String type,String key,String title,String message,String page){
  if(userId!=null && userId>0 && Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_user WHERE id=?)",Boolean.class,userId))){emit(userId,type,key,title,message,page);return;}
  var users=store.jdbc().queryForList("SELECT u.id FROM lin_user u JOIN lin_lb_listdiagnosisproject o ON o.InfoHospitalId=u.hospital_id LEFT JOIN lin_doctor_profile p ON p.user_id=u.id WHERE o.ListDiagnosisProjectId=? AND u.status='normal' AND (p.review_status IS NULL OR p.review_status='approved')",orderId);
  for(var user:users)emit(((Number)user.get("id")).intValue(),type,key,title,message,page);
 }
 @Transactional public void order(int orderId,Integer userId,int status){orderEvent(orderId,userId,status,null,null);}
 @Transactional public void reviewed(int orderId,Integer userId,int status,String reason,int reviewerId){orderEvent(orderId,userId,status,reason,reviewerId);}
 private void orderEvent(int orderId,Integer userId,int status,String reason,Integer reviewerId){
  var state=com.petlis.common.enums.OrderStatusEnum.fromCode(status);String label=state==null?"状态已更新":state.getDescription();
  Long legacyId=status==3?null:store.jdbc().queryForObject("SELECT MAX(id) FROM lin_lb_notification WHERE order_id=? AND new_status=? AND operation_time>=DATE_SUB(NOW(),INTERVAL 10 SECOND)",Long.class,orderId,status);
  String type=status==3?"report":"order",key=status==3?orderId+":published":legacyId==null?orderId+":"+status+":"+UUID.randomUUID():"legacy:"+legacyId;
  String message="送检单 #"+orderId+" 当前状态："+label+(reason==null || reason.isBlank()?"":"。审核意见："+reason);
  if(userId!=null && userId>0 && Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_user WHERE id=?)",Boolean.class,userId))){
   emit(userId,type,key,"送检单"+label,message,"pages/order-detail/order-detail?id="+orderId);
  }else{
   // Legacy PHP orders often have no doctor creator; notify only approved users of that order's hospital.
   var users=store.jdbc().queryForList("SELECT u.id FROM lin_user u JOIN lin_lb_listdiagnosisproject o ON o.InfoHospitalId=u.hospital_id LEFT JOIN lin_doctor_profile p ON p.user_id=u.id WHERE o.ListDiagnosisProjectId=? AND u.status='normal' AND (p.review_status IS NULL OR p.review_status='approved')",orderId);
   for(var user:users)emit(((Number)user.get("id")).intValue(),type,key,"送检单"+label,message,"pages/order-detail/order-detail?id="+orderId);
  }
  if(reviewerId!=null)store.jdbc().update("UPDATE lin_notification_event SET template_data=? WHERE event_type=? AND business_key=?",store.encode(Map.of("orderId",orderId,"reviewerId",reviewerId,"status",status,"reason",reason==null?"":reason)),type,key);
 }

 @Transactional public void article(long articleId,String title,String summary){
  store.jdbc().update("INSERT INTO lin_notification_event(user_id,event_type,business_key,title,message,page_path) SELECT u.id,'article',?,?,?,? FROM lin_user u WHERE u.status='normal' ON DUPLICATE KEY UPDATE id=id","article:"+articleId,title,summary.length()>1000?summary.substring(0,1000):summary,"pages/article-detail/article-detail?id="+articleId);
 }
 public Map<String,Object> inbox(boolean unread,int page,int size){String where=" FROM lin_notification_event WHERE user_id=?"+(unread?" AND read_at IS NULL":"");return store.page("SELECT COUNT(*)"+where,"SELECT id,event_type AS eventType,title,message,page_path AS pagePath,read_at AS readAt,created_at AS createdAt,push_status AS pushStatus"+where+" ORDER BY id DESC",page,size,SecurityUtils.getCurrentUserId());}
 public long unread(){Long count=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_notification_event WHERE user_id=? AND read_at IS NULL",Long.class,SecurityUtils.getCurrentUserId());return count==null?0:count;}
 @Transactional public void read(long id){store.one("SELECT id FROM lin_notification_event WHERE id=? AND user_id=?",id,SecurityUtils.getCurrentUserId());store.jdbc().update("UPDATE lin_notification_event SET read_at=COALESCE(read_at,NOW()) WHERE id=? AND user_id=?",id,SecurityUtils.getCurrentUserId());}
 @Transactional public void readAll(){store.jdbc().update("UPDATE lin_notification_event SET read_at=NOW() WHERE user_id=? AND read_at IS NULL",SecurityUtils.getCurrentUserId());}
 public List<Map<String,Object>> deliveries(long id){owned(id);return store.jdbc().queryForList("SELECT status,error_code AS errorCode,error_message AS errorMessage,created_at AS createdAt FROM lin_notification_delivery WHERE event_id=? ORDER BY id DESC LIMIT 20",id);}
 private void owned(long id){if(permission.isReviewer())store.one("SELECT id FROM lin_notification_event WHERE id=?",id);else store.one("SELECT id FROM lin_notification_event WHERE id=? AND user_id=?",id,SecurityUtils.getCurrentUserId());}
 @Transactional public void retry(long id){owned(id);int n=store.jdbc().update("UPDATE lin_notification_event SET push_status='pending',retry_count=0,next_attempt_at=NOW(),last_error='' WHERE id=? AND push_status IN ('failed','unconfigured','skipped')",id);if(n!=1)throw new BusinessException("该消息无需重试或正在处理");}
 public List<Map<String,Object>> templates(){if(!env.getProperty("app.notifications.push-enabled",Boolean.class,false))return List.of();var result=new ArrayList<Map<String,Object>>();for(String type:List.of("order","report","invoice","article","profile","wallet","mall")){String id=env.getProperty("app.notifications.templates."+type+".id","");if(!id.isBlank())result.add(Map.of("eventType",type,"templateId",id));}return result;}
 public int process(int limit){int processed=0;for(int i=0;i<Math.min(limit,50);i++){Boolean found=transactions.execute(tx->{
   var rows=store.jdbc().queryForList("SELECT * FROM lin_notification_event WHERE push_status IN ('pending','failed') AND retry_count<5 AND (next_attempt_at IS NULL OR next_attempt_at<=NOW()) ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED");if(rows.isEmpty())return false;
   deliver(rows.get(0));return true;
  });if(!Boolean.TRUE.equals(found))break;processed++;}return processed;}
 private void deliver(Map<String,Object> event){
  long id=((Number)event.get("id")).longValue();int userId=((Number)event.get("user_id")).intValue();String type=event.get("event_type").toString();
  var user=store.one("SELECT u.openid,u.status,p.order_notifications,p.report_notifications,p.invoice_notifications,p.news_notifications FROM lin_user u LEFT JOIN lin_user_preferences p ON p.user_id=u.id WHERE u.id=?",userId);
  String field=switch(type){case "order"->"order_notifications";case "report"->"report_notifications";case "invoice"->"invoice_notifications";case "article"->"news_notifications";default->null;};
  Object setting=field==null?null:user.get(field);boolean allowed=setting==null?!"article".equals(type):(setting instanceof Boolean b?b:((Number)setting).intValue()!=0);
  SubscriptionPush.Outcome result;
  if(!"normal".equals(user.get("status")) || !allowed)result=new SubscriptionPush.Outcome("skipped","PREFERENCE","用户未开启此类提醒",false);
  else if(user.get("openid")==null || user.get("openid").toString().isBlank())result=new SubscriptionPush.Outcome("skipped","NO_WECHAT","账号未绑定微信",false);
  else result=push.send(type,user.get("openid").toString(),event.get("title").toString(),event.get("message").toString(),event.get("page_path").toString());
  int attempts=((Number)event.get("retry_count")).intValue()+1;
  String status=result.status();if("failed".equals(status) && !result.retryable())status="skipped";
  store.jdbc().update("UPDATE lin_notification_event SET push_status=?,retry_count=?,next_attempt_at=?,last_error=? WHERE id=?",status,attempts,java.sql.Timestamp.from(Instant.now().plusSeconds(Math.min(3600,60L*(1L<<Math.min(attempts-1,6))))),result.message(),id);
  store.jdbc().update("INSERT INTO lin_notification_delivery(event_id,status,error_code,error_message) VALUES(?,?,?,?)",id,status,result.code(),result.message());
 }
}
