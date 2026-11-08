package com.petlis.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** PC orchestration; preparation commits in its own service before any later provider operation. */
@Service
public class LegacyBankPaymentApplicationService {
 private final FeatureStore store;
 private final AccessService access;
 private final LegacyBankPaymentQuoteService quotes;
 private final LegacyBankPaymentPreparationService preparation;
 private final String merchant;
 public LegacyBankPaymentApplicationService(FeatureStore store,AccessService access,LegacyBankPaymentQuoteService quotes,
         LegacyBankPaymentPreparationService preparation,@Value("${app.legacy-bank.merchant:}") String merchant){
  this.store=store;this.access=access;this.quotes=quotes;this.preparation=preparation;this.merchant=merchant;
 }
 @Transactional(readOnly=true)
 public Map<String,Boolean> capabilities(){
  access.requireAdmin();var result=new LinkedHashMap<>(quotes.capabilities());boolean allowed=false;
  var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-bank-payments' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");
  if(rows.size()==1)try{var value=new ObjectMapper().readTree(rows.get(0).get("config_value").toString()).path("prepareEnabled");allowed=value.isBoolean()&&value.asBoolean();}
  catch(Exception e){throw new BusinessException(409,"原网银业务参数无效");}
  result.put("bankPaymentCreate",Boolean.TRUE.equals(result.get("bankPayments"))&&allowed&&merchant!=null&&merchant.matches("[A-Za-z0-9]{1,20}"));
  return result;
 }
 public LegacyBankPaymentPreparationService.Prepared prepare(LegacyBankPaymentPreparationService.Command command){
  if(!capabilities().get("bankPaymentCreate"))throw new BusinessException(409,"支付单准备尚未开启或商户配置缺失");
  return preparation.prepare(command,merchant);
 }
 @Transactional(readOnly=true)
 public Map<String,Object> requests(int page,int size){
  requireRead();var result=new LinkedHashMap<>(store.page("SELECT COUNT(*) FROM lin_order_idempotency WHERE user_id=? AND operation='LEGACY_BANK_PREPARE'",
          "SELECT idempotency_key,status,response_body,created_at FROM lin_order_idempotency WHERE user_id=? AND operation='LEGACY_BANK_PREPARE' ORDER BY id DESC",page,size,access.current().userId()));
  @SuppressWarnings("unchecked") var rows=(List<Map<String,Object>>)result.get("items");
  result.put("items",rows.stream().map(this::receipt).toList());return result;
 }
 @Transactional(readOnly=true)
 public Map<String,Object> request(String key){
  requireRead();if(key==null||!key.matches("[A-Za-z0-9_-]{1,128}"))throw new BusinessException(400,"支付请求编号无效");
  return receipt(store.one("SELECT idempotency_key,status,response_body,created_at FROM lin_order_idempotency WHERE user_id=? AND operation='LEGACY_BANK_PREPARE' AND idempotency_key=?",access.current().userId(),key));
 }
 private void requireRead(){if(!Boolean.TRUE.equals(capabilities().get("bankPayments")))throw new BusinessException(409,"原网银业务尚未开启");}
 private Map<String,Object> receipt(Map<String,Object> row){
  try{
   var saved=new ObjectMapper().readValue(row.get("response_body").toString(),new TypeReference<Map<String,Object>>(){});
   // Only expose the business receipt; never raw provider responses or signing material.
   var result=new LinkedHashMap<String,Object>();for(String key:List.of("orderId","amount","amountFen","count"))result.put(key,saved.get(key));
   result.put("requestKey",row.get("idempotency_key"));result.put("status",row.get("status"));result.put("createdAt",row.get("created_at"));return result;
  }catch(Exception e){throw new BusinessException(409,"支付请求记录需人工核对，禁止重新创建");}
 }
}
