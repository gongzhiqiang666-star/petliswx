package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;import com.petlis.common.exception.BusinessException;import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.math.*;import java.time.*;import java.util.*;
/** Read-only staff parity preview of Project/View. Customer entry policy is not decided here. */
@Service @RequiredArgsConstructor
public class LegacyCustomerViewService {
 private final FeatureStore store;private final AccessService access;private final LegacyReportLinks links;
 public record Command(String kind,Long id,String token){}
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-customer-views' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");boolean enabled=false;if(rows.size()==1)try{var value=new ObjectMapper().readTree(rows.get(0).get("config_value").toString()).path("enabled");enabled=value.isBoolean()&&value.asBoolean();}catch(Exception e){throw new BusinessException(409,"客户账单查看参数无效");}return Map.of("legacyCustomerViews",enabled);}
 @Transactional(readOnly=true) public Map<String,Object> preview(Command command){return previewAt(command,LocalDate.now(ZoneId.of("Asia/Shanghai")));}
 Map<String,Object> previewAt(Command command,LocalDate today){
  if(!capabilities().get("legacyCustomerViews"))throw new BusinessException(409,"客户账单核对尚未开启");
  if(command==null||!Set.of("CID","HOSPITAL").contains(Objects.toString(command.kind(),""))||command.id()!=null&&command.token()!=null)throw new BusinessException(400,"请选择明细或医院及唯一编号参数");
  long id=command.id()==null?links.decodeId(command.token()):command.id();if(id<1||id>4294967295L)throw new BusinessException(400,"编号无效");
  var result=new LinkedHashMap<String,Object>();result.put("kind",command.kind());result.put("previewOnly",true);result.put("needPay",false);result.put("reportAllowed",false);result.put("nextAction","HOSPITAL_BILL");result.put("message","可保存此链接,以便下次查阅");
  long hospitalId=id;
  if("CID".equals(command.kind())){
   var detail=store.one("SELECT DiagnosisDetailId,ListDiagnosisProjectId,InfoTestTypeId,PDFHttpUrl FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=?",id);
   store.one("SELECT InfoTestTypeId FROM lin_lb_infotesttype WHERE InfoTestTypeId=?",detail.get("InfoTestTypeId"));
   var order=store.one("SELECT ListDiagnosisProjectId,InfoHospitalId,InfoAnimalId,UserNO,AnimalName,MasterName,IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",detail.get("ListDiagnosisProjectId"));
   var species=store.one("SELECT AnimalName FROM lin_lb_infoanimal WHERE InfoAnimalId=?",order.get("InfoAnimalId"));hospitalId=((Number)order.get("InfoHospitalId")).longValue();var hospital=hospital(hospitalId);
   var items=store.jdbc().queryForList("SELECT DiagnosisDetailId,TestName,IsCheckPass,PayStatus2,CAST(TestPrice AS CHAR) AS TestPrice,ReceiveSampleDt FROM vwdiagnosisdetail WHERE ListDiagnosisProjectId=? AND IsCheckPass=0",order.get("ListDiagnosisProjectId"));
   for(var item:items){item.put("paymentLabel",LegacyFinanceService.payLabel(number(item,"IsCheckPass"),number(item,"PayStatus2")));item.put("displayPrice",display(item.get("TestPrice")));}
   order.put("speciesName",species.get("AnimalName"));order.put("paymentLabel",LegacyFinanceService.payLabel(number(order,"IsPayMoney"),number(order,"IsPayMoney")));result.put("project",order);result.put("items",items);result.put("detailId",id);
   boolean needPay;
   if(number(hospital,"PayType")==1){var cutoff=LegacyCustomerViewRules.debtCutoff(today);result.put("debtCutoff",cutoff);needPay=Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM vwdiagnosisdetail WHERE InfoHospitalId=? AND IsCheckPass=0 AND ReceiveSampleDt BETWEEN '2018-09-01 00:00:00' AND ? AND PayStatus2=0)",Boolean.class,hospitalId,cutoff));result.put("message","您是月结账户,按月支付账单,即可查看报告!");}
   else{
    BigDecimal amount=store.jdbc().queryForObject("SELECT COALESCE(SUM(TestPrice),0) FROM vwdiagnosisdetail WHERE ListDiagnosisProjectId=? AND IsCheckPass=0 AND PayStatus2=0 AND (InfoTestTypeId=246 OR ReceiveSampleDt>'1970-01-01 00:00:00')",BigDecimal.class,order.get("ListDiagnosisProjectId"));if(amount==null)amount=BigDecimal.ZERO;needPay=amount.signum()>0;result.put("amount",display(amount));if(needPay)result.put("message","您有账单待支付,支付后刷新此页即可查看报告! ");
   }
   boolean allowed=!needPay&&detail.get("PDFHttpUrl")!=null&&!detail.get("PDFHttpUrl").toString().isEmpty()&&!"0".equals(detail.get("PDFHttpUrl").toString());result.put("needPay",needPay);result.put("reportAllowed",allowed);result.put("nextAction",allowed?"REPORT":needPay?(number(hospital,"PayType")==1?"HOSPITAL_BILL":"ORDER_PAYMENT"):"REPORT_PENDING");if(allowed)result.put("reportReference",detail.get("PDFHttpUrl"));
  }
  var hospital=hospital(hospitalId);result.put("hospital",hospital);result.put("hospitalId",hospitalId);
  var months=store.jdbc().queryForList("SELECT LEFT(ReceiveSampleDt,7) AS mouth,CAST(SUM(IF(PayStatus2=0,TestPrice,0)) AS CHAR) AS totalNoPayFee,CAST(SUM(IF(PayStatus2<>0,TestPrice,0)) AS CHAR) AS totalHasPayFee,CAST(SUM(TestPrice) AS CHAR) AS TestPrice FROM vwdiagnosisdetail WHERE InfoHospitalId=? AND IsCheckPass=0 AND ReceiveSampleDt>='2018-09-01 00:00:00' GROUP BY LEFT(ReceiveSampleDt,7) ORDER BY mouth DESC",hospitalId);
  for(var month:months)for(String key:List.of("totalNoPayFee","totalHasPayFee","TestPrice"))month.put(key,display(month.get(key)));result.put("months",months);return result;
 }
 private Map<String,Object> hospital(long id){if(id<1)throw new BusinessException(400,"医院编号无效");return store.one("SELECT InfoHospitalId,HospitalName,PayType FROM lin_lb_infohospital WHERE InfoHospitalId=?",id);}
 private static int number(Map<String,Object> row,String key){Object value=row.get(key);return value==null?0:((Number)value).intValue();}
 private static String display(Object amount){return LegacyPricingRules.display(amount==null?"0":amount instanceof BigDecimal value?value.toPlainString():amount.toString());}
}
