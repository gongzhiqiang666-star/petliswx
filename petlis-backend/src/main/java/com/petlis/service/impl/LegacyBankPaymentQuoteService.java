package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;import java.time.*;import java.util.*;
/** Source Project/Pay.index selection and per-step bcadd(...,2), without creating a payment. */
@Service @RequiredArgsConstructor
public class LegacyBankPaymentQuoteService {
 private final FeatureStore store;private final AccessService access;
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-bank-payments' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");boolean enabled=false;if(rows.size()==1)try{var v=new ObjectMapper().readTree(rows.get(0).get("config_value").toString()).path("enabled");enabled=v.isBoolean()&&v.asBoolean();}catch(Exception e){throw new BusinessException(409,"原网银业务参数无效");}return Map.of("bankPayments",enabled,"bankPaymentCreate",false,"bankPaymentProviderQuery",false,"bankPaymentRefund",false);}
 @Transactional(readOnly=true) public Map<String,Object> quote(String mode,Long hospital,String month,String orderNo){
  return calculate(mode,hospital,month,orderNo,false);
 }
 Map<String,Object> quoteLocked(String mode,Long hospital,String month,String orderNo){
  if(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw new BusinessException(409,"核价锁定需要事务");
  return calculate(mode,hospital,month,orderNo,true);
 }
 private Map<String,Object> calculate(String mode,Long hospital,String month,String orderNo,boolean lock){
  if(!capabilities().get("bankPayments"))throw new BusinessException(409,"原网银业务尚未开启");String where,description,period="";var args=new ArrayList<Object>();
  if("MONTH".equals(mode)){
   if(hospital==null||hospital<1||hospital>4294967295L)throw new BusinessException(400,"请选择有效医院");YearMonth ym;try{if(month==null||!month.matches("[0-9]{4}-[0-9]{2}"))throw new IllegalArgumentException();ym=YearMonth.parse(month);if(ym.getYear()<1000||ym.getYear()>9999)throw new IllegalArgumentException();}catch(Exception e){throw new BusinessException(400,"月份格式应为YYYY-MM");}
   var hosp=store.one("SELECT HospitalName FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital);var start=ym.atDay(1).atStartOfDay();var end=ym.atEndOfMonth().atTime(23,59,59);
   where="InfoHospitalId=? AND IsCheckPass=0 AND PayStatus2=0 AND ReceiveSampleDt>=? AND ReceiveSampleDt<?";Collections.addAll(args,hospital,start,end);description="账单:"+ym.atDay(1)+"至"+ym.atEndOfMonth()+"_"+hosp.get("HospitalName");period=start.toString().replace('T',' ')+" ≤ 收样时间 < "+end.toString().replace('T',' ');
  }else if("ORDER".equals(mode)){
   if(orderNo==null||orderNo.trim().isEmpty()||orderNo.length()>250)throw new BusinessException(400,"请填写有效送检单号");orderNo=orderNo.trim();where="UserNO=? AND IsCheckPass=0 AND PayStatus2=0 AND (InfoTestTypeId=246 OR ReceiveSampleDt<>'1900-01-01 00:00:00')";args.add(orderNo);description="订单:"+orderNo;
  }else throw new BusinessException(400,"请选择按单号或按医院月份核价");
  var items=store.jdbc().queryForList("SELECT DiagnosisDetailId,ListDiagnosisProjectId,InfoHospitalId,UserNO,AnimalName,TestName,InfoTestTypeId,CAST(TestPrice AS CHAR) AS TestPrice,ReceiveSampleDt FROM vwdiagnosisdetail WHERE "+where+" ORDER BY DiagnosisDetailId"+(lock?" FOR UPDATE":""),args.toArray());BigDecimal total=new BigDecimal("0.00");var hospitals=new HashSet<Object>();
  for(var row:items){Object value=row.get("TestPrice");if(value==null)throw new BusinessException(409,"原待付明细金额为空，请先核对");total=total.add(new BigDecimal(value.toString())).setScale(2,RoundingMode.DOWN);hospitals.add(row.get("InfoHospitalId"));}
  var result=new LinkedHashMap<String,Object>();result.put("mode",mode);result.put("description",description);result.put("period",period);result.put("amount",total.toPlainString());result.put("displayAmount",LegacyPricingRules.display(total.toPlainString()));result.put("amountFen",total.movePointRight(2).setScale(0,RoundingMode.DOWN).toPlainString());result.put("count",items.size());result.put("items",items);result.put("multipleHospitals",hospitals.size()>1);result.put("createsPayment",false);return result;
 }
}
