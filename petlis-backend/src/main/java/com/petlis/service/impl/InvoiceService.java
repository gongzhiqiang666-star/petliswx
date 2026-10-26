package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor
public class InvoiceService {
 private final BusinessNotifications notifications;

 private final FeatureStore store;
 private final ReviewPermission permission;
 private final com.fasterxml.jackson.databind.ObjectMapper json;
 public record Profile(String title,String taxNumber,String email,String address,String phone,String bankName,String bankAccount){}
 private void validate(Profile r){if(r.title()==null || r.title().isBlank() || r.title().length()>200 || r.email()==null || !r.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || r.email().length()>200 || (r.taxNumber()!=null && !r.taxNumber().isBlank() && !r.taxNumber().matches("[A-Za-z0-9]{15,20}")))throw new BusinessException("请填写有效发票抬头、税号和邮箱");}
 public List<Map<String,Object>> profiles(){return store.jdbc().queryForList("SELECT id,title,tax_number AS taxNumber,email,address,phone,bank_name AS bankName,bank_account AS bankAccount FROM lin_invoice_profile WHERE user_id=? ORDER BY id DESC",SecurityUtils.getCurrentUserId());}
 @Transactional public long addProfile(Profile r){validate(r);return store.insert("INSERT INTO lin_invoice_profile(user_id,title,tax_number,email,address,phone,bank_name,bank_account) VALUES(?,?,?,?,?,?,?,?)",SecurityUtils.getCurrentUserId(),r.title(),orEmpty(r.taxNumber()),r.email(),orEmpty(r.address()),orEmpty(r.phone()),orEmpty(r.bankName()),orEmpty(r.bankAccount()));}
 @Transactional public void updateProfile(long id,Profile r){validate(r);int count=store.jdbc().update("UPDATE lin_invoice_profile SET title=?,tax_number=?,email=?,address=?,phone=?,bank_name=?,bank_account=? WHERE id=? AND user_id=?",r.title(),orEmpty(r.taxNumber()),r.email(),orEmpty(r.address()),orEmpty(r.phone()),orEmpty(r.bankName()),orEmpty(r.bankAccount()),id,SecurityUtils.getCurrentUserId());if(count!=1)throw new BusinessException(404,"开票信息不存在");}
 public Map<String,Object> eligible(int page,int size){String where=" FROM lin_lb_listdiagnosisproject p LEFT JOIN lin_invoice_order i ON i.order_id=p.ListDiagnosisProjectId WHERE p.CreateSampleId=? AND p.InfoHospitalId=? AND p.IsPayMoney=1 AND i.order_id IS NULL";return store.page("SELECT COUNT(*)"+where,"SELECT p.ListDiagnosisProjectId AS orderId,p.UserNO AS orderNo,p.AnimalName AS animalName,p.TestFee AS amount,p.CreateDt AS createdAt"+where+" ORDER BY p.ListDiagnosisProjectId DESC",page,size,SecurityUtils.getCurrentUserId(),SecurityUtils.getCurrentHospitalId());}
 public record Application(long profileId,List<Integer> orderIds,String requestKey){}
 @Transactional public long apply(Application r){
  if(r.requestKey()==null || !r.requestKey().matches("[A-Za-z0-9_-]{16,64}") || r.orderIds()==null || r.orderIds().isEmpty() || r.orderIds().size()>50 || r.orderIds().stream().anyMatch(Objects::isNull) || new HashSet<>(r.orderIds()).size()!=r.orderIds().size())throw new BusinessException("开票请求无效");
  int userId=SecurityUtils.getCurrentUserId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",userId);
  var existing=store.jdbc().queryForList("SELECT id,profile_snapshot FROM lin_invoice_application WHERE user_id=? AND request_key=?",userId,r.requestKey());
  if(!existing.isEmpty()){
   var first=existing.get(0);try{var snapshot=json.readTree(first.get("profile_snapshot").toString());var original=new java.util.TreeSet<Integer>();for(var item:snapshot.path("orderIds"))original.add(item.asInt());if(snapshot.path("profileId").asLong(-1)!=r.profileId() || !original.equals(new java.util.TreeSet<>(r.orderIds())))throw new BusinessException(409,"重复请求的开票抬头或订单不一致");}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"原申请快照无法校验，请使用新的申请编号");}
   return ((Number)first.get("id")).longValue();
  }
  var profile=store.one("SELECT title,tax_number,email,address,phone,bank_name,bank_account FROM lin_invoice_profile WHERE id=? AND user_id=?",r.profileId(),userId);
  BigDecimal amount=BigDecimal.ZERO;List<Integer> sorted=new ArrayList<>(r.orderIds());Collections.sort(sorted);
  for(int id:sorted){var order=store.one("SELECT TestFee,IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? AND CreateSampleId=? AND InfoHospitalId=? FOR UPDATE",id,userId,SecurityUtils.getCurrentHospitalId());if(((Number)order.get("IsPayMoney")).intValue()!=1)throw new BusinessException("仅已支付送检单可申请开票");if(Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM lin_invoice_order WHERE order_id=?)",Boolean.class,id)))throw new BusinessException("订单已有开票申请");amount=amount.add(new BigDecimal(order.get("TestFee").toString()).setScale(2,java.math.RoundingMode.HALF_UP));}
  if(amount.signum()<=0)throw new BusinessException("可开票金额无效");
  profile=new java.util.LinkedHashMap<>(profile);profile.put("profileId",r.profileId());profile.put("orderIds",sorted);
  long id=store.insert("INSERT INTO lin_invoice_application(user_id,profile_snapshot,amount,request_key) VALUES(?,?,?,?)",userId,store.encode(profile),amount,r.requestKey());
  for(int orderId:sorted)store.jdbc().update("INSERT INTO lin_invoice_order(application_id,order_id) VALUES(?,?)",id,orderId);
  return id;
 }
 public Map<String,Object> list(int page,int size){return store.page("SELECT COUNT(*) FROM lin_invoice_application WHERE user_id=?","SELECT id,amount,status,invoice_url AS invoiceUrl,reject_reason AS rejectReason,issued_at AS issuedAt,created_at AS createdAt FROM lin_invoice_application WHERE user_id=? ORDER BY id DESC",page,size,SecurityUtils.getCurrentUserId());}
 public Map<String,Object> detail(long id){return store.one("SELECT id,profile_snapshot AS profileSnapshot,amount,status,invoice_url AS invoiceUrl,reject_reason AS rejectReason,issued_at AS issuedAt,created_at AS createdAt FROM lin_invoice_application WHERE id=? AND user_id=?",id,SecurityUtils.getCurrentUserId());}
 @Transactional public void issue(long id,String invoiceUrl){permission.requireReviewer();if(invoiceUrl==null || !invoiceUrl.startsWith("https://") || invoiceUrl.length()>1000)throw new BusinessException("请提供有效电子发票地址");var record=store.one("SELECT status,user_id FROM lin_invoice_application WHERE id=? FOR UPDATE",id);if(!"pending".equals(record.get("status")))throw new BusinessException("发票申请已处理");store.jdbc().update("UPDATE lin_invoice_application SET status='issued',invoice_url=?,issued_at=NOW() WHERE id=?",invoiceUrl,id);notifications.emit(((Number)record.get("user_id")).intValue(),"invoice","issued:"+id,"电子发票已开具","开票申请 #"+id+" 已开具，可前往下载。","pages/invoices/invoices");}
 @Transactional public void reject(long id,String reason){permission.requireReviewer();if(reason==null || reason.isBlank() || reason.length()>500)throw new BusinessException("请填写驳回原因");var record=store.one("SELECT status,user_id FROM lin_invoice_application WHERE id=? FOR UPDATE",id);if(!"pending".equals(record.get("status")))throw new BusinessException("发票申请已处理");store.jdbc().update("UPDATE lin_invoice_application SET status='rejected',reject_reason=? WHERE id=?",reason,id);store.jdbc().update("DELETE FROM lin_invoice_order WHERE application_id=?",id);notifications.emit(((Number)record.get("user_id")).intValue(),"invoice","rejected:"+id,"开票申请未通过",reason,"pages/invoices/invoices");}
 private String orEmpty(String value){return value==null?"":value;}
}
