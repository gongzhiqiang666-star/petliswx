package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;import com.petlis.common.exception.BusinessException;import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.math.*;import java.time.*;import java.util.*;
/** Read-only staff parity preview of Project/View. Customer entry policy is not decided here. */
@Service @RequiredArgsConstructor
public class LegacyCustomerViewService {
 private final FeatureStore store;private final AccessService access;private final LegacyReportLinks links;
 public record Command(String kind,Long id,String token){}
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-customer-views' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");boolean enabled=false,download=false;if(rows.size()==1)try{var value=new ObjectMapper().readTree(rows.get(0).get("config_value").toString()).path("enabled");enabled=value.isBoolean()&&value.asBoolean();var fileFlag=new ObjectMapper().readTree(rows.get(0).get("config_value").toString()).path("reportDownloadEnabled");download=fileFlag.isBoolean()&&fileFlag.asBoolean();}catch(Exception e){throw new BusinessException(409,"客户账单查看参数无效");}return Map.of("legacyCustomerViews",enabled,"legacyCustomerReportDownload",enabled&&download);}
 @Transactional(readOnly=true) public Map<String,Object> preview(Command command){return previewAt(command,LocalDate.now(ZoneId.of("Asia/Shanghai")));}
 Map<String,Object> previewAt(Command command,LocalDate today){
  if(!capabilities().get("legacyCustomerViews"))throw new BusinessException(409,"客户账单核对尚未开启");
  if(command==null||!Set.of("CID","HOSPITAL").contains(Objects.toString(command.kind(),""))||command.id()!=null&&command.token()!=null)throw new BusinessException(400,"请选择明细或医院及唯一编号参数");
  long id=command.id()==null?links.decodeId(command.token()):command.id();if(id<1||id>4294967295L)throw new BusinessException(400,"编号无效");
  var result=new LinkedHashMap<String,Object>();result.put("kind",command.kind());result.put("previewOnly",true);result.put("needPay",false);result.put("reportAllowed",false);result.put("nextAction","HOSPITAL_BILL");result.put("message","可保存此链接,以便下次查阅");
  long hospitalId=id;
  if("CID".equals(command.kind())){
   var permission=reportPermissionAt(id,today);result.putAll(permission);hospitalId=((Number)permission.get("hospitalId")).longValue();
  }
  var hospital=hospital(hospitalId);result.put("hospital",hospital);result.put("hospitalId",hospitalId);
  var months=store.jdbc().queryForList("SELECT LEFT(ReceiveSampleDt,7) AS mouth,CAST(SUM(IF(PayStatus2=0,TestPrice,0)) AS CHAR) AS totalNoPayFee,CAST(SUM(IF(PayStatus2<>0,TestPrice,0)) AS CHAR) AS totalHasPayFee,CAST(SUM(TestPrice) AS CHAR) AS TestPrice FROM vwdiagnosisdetail WHERE InfoHospitalId=? AND IsCheckPass=0 AND ReceiveSampleDt>='2018-09-01 00:00:00' GROUP BY LEFT(ReceiveSampleDt,7) ORDER BY mouth DESC",hospitalId);
  for(var month:months)for(String key:List.of("totalNoPayFee","totalHasPayFee","TestPrice"))month.put(key,display(month.get(key)));result.put("months",months);return result;
 }
 // Internal rule reuse: callers must independently authorize hospital ownership before calling.
 Map<String,Object> reportPermissionAt(long id,LocalDate today){
  var result=new LinkedHashMap<String,Object>();
   var detail=store.one("SELECT DiagnosisDetailId,ListDiagnosisProjectId,InfoTestTypeId,PDFHttpUrl FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=?",id);
   store.one("SELECT InfoTestTypeId FROM lin_lb_infotesttype WHERE InfoTestTypeId=?",detail.get("InfoTestTypeId"));
   var content=projectContent(true,detail.get("ListDiagnosisProjectId"));
   @SuppressWarnings("unchecked")var order=(Map<String,Object>)content.get("project");
   @SuppressWarnings("unchecked")var hospital=(Map<String,Object>)content.get("hospital");long hospitalId=((Number)order.get("InfoHospitalId")).longValue();result.put("hospitalId",hospitalId);result.putAll(content);result.put("detailId",id);
   boolean needPay;
   if(number(hospital,"PayType")==1){var cutoff=LegacyCustomerViewRules.debtCutoff(today);result.put("debtCutoff",cutoff);needPay=Boolean.TRUE.equals(store.jdbc().queryForObject("SELECT EXISTS(SELECT 1 FROM vwdiagnosisdetail WHERE InfoHospitalId=? AND IsCheckPass=0 AND ReceiveSampleDt BETWEEN '2018-09-01 00:00:00' AND ? AND PayStatus2=0)",Boolean.class,hospitalId,cutoff));result.put("message","您是月结账户,按月支付账单,即可查看报告!");}
   else{
    BigDecimal amount=store.jdbc().queryForObject("SELECT COALESCE(SUM(TestPrice),0) FROM vwdiagnosisdetail WHERE ListDiagnosisProjectId=? AND IsCheckPass=0 AND PayStatus2=0 AND (InfoTestTypeId=246 OR ReceiveSampleDt>'1970-01-01 00:00:00')",BigDecimal.class,order.get("ListDiagnosisProjectId"));if(amount==null)amount=BigDecimal.ZERO;needPay=amount.signum()>0;result.put("amount",display(amount));if(needPay)result.put("message","您有账单待支付,支付后刷新此页即可查看报告! ");
   }
   boolean allowed=!needPay&&detail.get("PDFHttpUrl")!=null&&!detail.get("PDFHttpUrl").toString().isEmpty()&&!"0".equals(detail.get("PDFHttpUrl").toString());result.put("needPay",needPay);result.put("reportAllowed",allowed);result.put("nextAction",allowed?"REPORT":needPay?(number(hospital,"PayType")==1?"HOSPITAL_BILL":"ORDER_PAYMENT"):"REPORT_PENDING");if(allowed)result.put("reportReference",detail.get("PDFHttpUrl"));
  return result;
 }
 public record MonthCommand(Long hospitalId,String token,String month,String userNo){}
 @Transactional(readOnly=true) public Map<String,Object> month(MonthCommand c){
  if(!capabilities().get("legacyCustomerViews"))throw new BusinessException(409,"客户账单核对尚未开启");
  if(c==null||c.hospitalId()!=null&&c.token()!=null)throw new BusinessException(400,"月份查询参数无效");
  long id=c.hospitalId()==null?links.decodeId(c.token()):c.hospitalId();if(id<1||id>4294967295L)throw new BusinessException(400,"医院编号无效");
  YearMonth month;try{if(c.month()==null||!c.month().matches("[0-9]{4}-[0-9]{2}"))throw new IllegalArgumentException();month=YearMonth.parse(c.month());if(month.getYear()<1000||month.getYear()>9999)throw new IllegalArgumentException();}catch(Exception e){throw new BusinessException(400,"月份格式应为YYYY-MM");}
  String userNo=c.userNo()==null?"":c.userNo().trim();if(userNo.length()>250)throw new BusinessException(400,"样本编号过长");
  var start=month.atDay(1).atStartOfDay();var end=month.atEndOfMonth().atTime(23,59,59);String from=" FROM vwdiagnosisdetail WHERE InfoHospitalId=? AND ReceiveSampleDt BETWEEN ? AND ?";
  // ANY_VALUE keeps the original nonaggregated/unspecified representative, without changing SQL mode.
  var orders=store.jdbc().queryForList("SELECT UserNO,CAST(SUM(IF(PayStatus2=0,TestPrice,0)) AS CHAR) AS totalNoPayFee,CAST(SUM(IF(PayStatus2<>0,TestPrice,0)) AS CHAR) AS totalHasPayFee,CAST(SUM(TestPrice) AS CHAR) AS TestPrice,ANY_VALUE(IsPayMoney) AS IsPayMoney,ANY_VALUE(IsCheckPass) AS IsCheckPass,ANY_VALUE(LEFT(ReceiveSampleDt,10)) AS ReceiveSampleDt,ANY_VALUE(LEFT(SendMailDt,10)) AS SendMailDt,COUNT(DISTINCT COALESCE(CAST(IsPayMoney AS CHAR),'NULL')) AS payVariants,COUNT(DISTINCT COALESCE(CAST(IsCheckPass AS CHAR),'NULL')) AS exemptVariants,COUNT(DISTINCT COALESCE(LEFT(ReceiveSampleDt,10),'NULL')) AS receiveVariants,COUNT(DISTINCT COALESCE(LEFT(SendMailDt,10),'NULL')) AS reportVariants,COUNT(DISTINCT ListDiagnosisProjectId) AS parentVariants"+from+" GROUP BY UserNO ORDER BY ANY_VALUE(ListDiagnosisProjectId) DESC LIMIT 100",id,start,end);
  for(var row:orders){for(String key:List.of("totalNoPayFee","totalHasPayFee","TestPrice"))row.put(key,display(row.get(key)));boolean mixed=false;for(String key:List.of("payVariants","exemptVariants","receiveVariants","reportVariants","parentVariants"))mixed|=number(row,key)>1;row.put("representativeAmbiguous",mixed);row.put("paymentLabel",row.get("IsPayMoney")==null&&number(row,"IsCheckPass")!=1?"":LegacyFinanceService.payLabel(number(row,"IsCheckPass"),number(row,"IsPayMoney")));String date=Objects.toString(row.get("SendMailDt"),"");row.put("reportDateLabel",date.compareTo("1970-01-01")<0?"检测中":date);}
  Long total=store.jdbc().queryForObject("SELECT COUNT(*) FROM (SELECT UserNO"+from+" GROUP BY UserNO) grouped_orders",Long.class,id,start,end);
  var result=new LinkedHashMap<String,Object>();result.put("kind","MONTH");result.put("previewOnly",true);result.put("hospitalId",id);var requestedHospital=hospital(id);result.put("hospital",requestedHospital);result.put("queryHospital",requestedHospital);result.put("paymonth",c.month());result.put("orders",orders);result.put("totalGroups",total==null?0:total);result.put("limited",total!=null&&total>100);result.put("userNo",userNo);
  // Original loaduserno deliberately has no hospital/month predicate. Only ADMIN can use this preview.
  if(!userNo.isEmpty()&&!"0".equals(userNo)){var content=projectContent(false,userNo);result.putAll(content);result.put("contextHospitalId",id);@SuppressWarnings("unchecked")var actual=(Map<String,Object>)content.get("hospital");@SuppressWarnings("unchecked")var items=(List<Map<String,Object>>)content.get("items");result.put("crossHospitalSource",((Number)actual.get("InfoHospitalId")).longValue()!=id||items.stream().anyMatch(row->((Number)row.get("InfoHospitalId")).longValue()!=id));}
  return result;
 }
 private Map<String,Object> projectContent(boolean byId,Object value){String column=byId?"ListDiagnosisProjectId":"UserNO";var order=store.one("SELECT ListDiagnosisProjectId,InfoHospitalId,InfoAnimalId,UserNO,AnimalName,MasterName,IsPayMoney FROM lin_lb_listdiagnosisproject WHERE "+column+"=? LIMIT 1",value);var species=store.one("SELECT AnimalName FROM lin_lb_infoanimal WHERE InfoAnimalId=?",order.get("InfoAnimalId"));var hospital=hospital(((Number)order.get("InfoHospitalId")).longValue());var items=store.jdbc().queryForList("SELECT DiagnosisDetailId,ListDiagnosisProjectId,InfoHospitalId,UserNO,TestName,IsCheckPass,PayStatus2,CAST(TestPrice AS CHAR) AS TestPrice,ReceiveSampleDt FROM vwdiagnosisdetail WHERE "+column+"=? AND IsCheckPass=0",value);for(var item:items){item.put("paymentLabel",LegacyFinanceService.payLabel(number(item,"IsCheckPass"),number(item,"PayStatus2")));item.put("displayPrice",display(item.get("TestPrice")));}order.put("speciesName",species.get("AnimalName"));order.put("paymentLabel",LegacyFinanceService.payLabel(number(order,"IsPayMoney"),number(order,"IsPayMoney")));return Map.of("project",order,"hospital",hospital,"items",items);}
 private Map<String,Object> hospital(long id){if(id<1)throw new BusinessException(400,"医院编号无效");return store.one("SELECT InfoHospitalId,HospitalName,PayType FROM lin_lb_infohospital WHERE InfoHospitalId=?",id);}
 private static int number(Map<String,Object> row,String key){Object value=row.get(key);return value==null?0:((Number)value).intValue();}
 private static String display(Object amount){return LegacyPricingRules.display(amount==null?"0":amount instanceof BigDecimal value?value.toPlainString():amount.toString());}
}
