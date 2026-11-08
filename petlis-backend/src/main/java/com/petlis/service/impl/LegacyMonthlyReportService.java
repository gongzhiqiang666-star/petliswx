package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
@Service @RequiredArgsConstructor
public class LegacyMonthlyReportService {
 private final FeatureStore store;private final AccessService access;private final LegacyReportLinks links;
 private void read(){if(!capabilities().get("legacyMonthlyReports"))throw new BusinessException(409,"原医院月报尚未开启");}
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-monthly-reports' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");boolean read=false;if(rows.size()==1)try{var j=new ObjectMapper().readTree(rows.get(0).get("config_value").toString()).path("enabled");read=j.isBoolean()&&j.asBoolean();}catch(Exception e){throw new BusinessException(409,"原医院月报参数无效");}return Map.of("legacyMonthlyReports",read,"legacyMonthlyReportSend",false);}
 private YearMonth month(String value){try{if(value==null||!value.matches("[0-9]{4}-[0-9]{2}"))throw new IllegalArgumentException();return YearMonth.parse(value);}catch(Exception e){throw new BusinessException(400,"月份格式应为YYYY-MM");}}
 @Transactional(readOnly=true) public Map<String,Object> list(Long hospital,String fromMonth,String toMonth,Integer notified,int page,int size){
  read();if(page<0||size<1||size>100||hospital!=null&&(hospital<1||hospital>Integer.MAX_VALUE)||notified!=null&&notified!=0&&notified!=1)throw new BusinessException(400,"月报查询无效");
  String where=" FROM vw_hospmonthreport WHERE InfoHospitalId<>0";var args=new ArrayList<Object>();if(hospital!=null){where+=" AND InfoHospitalId=?";args.add(hospital);}YearMonth start=fromMonth==null||fromMonth.isEmpty()?null:month(fromMonth),end=toMonth==null||toMonth.isEmpty()?null:month(toMonth);if(start!=null&&end!=null&&start.isAfter(end))throw new BusinessException(400,"起始月份不能晚于结束月份");if(start!=null){where+=" AND mouth>=?";args.add(start.toString());}if(end!=null){where+=" AND mouth<=?";args.add(end.toString());}if(notified!=null){where+=" AND isnotice=?";args.add(notified);}
  String countSql="SELECT COUNT(*)"+where,listSql="SELECT 医院名称 AS hospitalName,InfoHospitalId AS hospitalId,月份 AS month,CAST(未付金额 AS CHAR) AS unpaid,CAST(已付金额 AS CHAR) AS paid,CAST(忽略金额 AS CHAR) AS ignored,是否已通知 AS notified,通知时间 AS notifiedAt"+where+" ORDER BY mouth DESC,InfoHospitalId LIMIT ? OFFSET ?";
  return store.jdbc().execute((org.springframework.jdbc.core.ConnectionCallback<Map<String,Object>>)connection->LegacyGroupingSession.query(connection,()->{
   var local=new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true));
   Long total=local.queryForObject(countSql,Long.class,args.toArray());var paged=new ArrayList<Object>(args);paged.add(size);paged.add((long)page*size);
   return Map.of("items",local.queryForList(listSql,paged.toArray()),"total",total==null?0:total,"page",page,"size",size);
  }));
 }
 @Transactional(readOnly=true) public Map<String,Object> templates(String kind){read();String table=switch(kind){case "report"->"lin_lb_dictfinancereport";case "mail"->"lin_lb_dictfinancemailreport";default->throw new BusinessException(400,"未知财务模板类型");};var rows=store.jdbc().queryForList("SELECT id,model_name FROM "+table+" ORDER BY id");for(var row:rows)row.put("model_name",LegacyDictionarySelector.display(row.get("model_name")));return Map.of("items",rows);}
 private Map<String,Object> template(String kind,Long id){String table=kind.equals("report")?"lin_lb_dictfinancereport":"lin_lb_dictfinancemailreport";if(id!=null&&id<1)throw new BusinessException(400,"模板编号无效");var rows=store.jdbc().queryForList("SELECT id,model_content FROM "+table+(id==null?"":" WHERE id=?"),id==null?new Object[]{}:new Object[]{id});if(id!=null&&rows.isEmpty())throw new BusinessException(404,"财务模板不存在");return rows.isEmpty()?Map.of("id",0,"model_content",""):rows.get(id==null?rows.size()-1:0);}
 @Transactional(readOnly=true) public Map<String,Object> draft(long hospital,String period,Long reportTemplate,Long mailTemplate){
  read();if(hospital<1||hospital>Integer.MAX_VALUE)throw new BusinessException(400,"医院编号无效");YearMonth m=month(period);LocalDate start=m.atDay(1),end=m.atEndOfMonth();var hosp=store.one("SELECT InfoHospitalId,HospitalName FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital);
  var rows=store.jdbc().queryForList("SELECT v.DiagnosisDetailId,v.UserNO,v.ReportNO,v.AnimalName,v.MasterName,v.TestName,v.IsCheckPass,v.PayStatus2,CAST(v.TestPrice AS CHAR) AS TestPrice,v.ReceiveSampleDt,v.SendMailDt,a.AnimalName AS AnimalIdName FROM vwdiagnosisdetail v LEFT JOIN lin_lb_infoanimal a ON a.InfoAnimalId=v.InfoAnimalId WHERE v.InfoHospitalId=? AND v.ReceiveSampleDt>=? AND v.ReceiveSampleDt<=? ORDER BY v.ListDiagnosisProjectId DESC,v.CreateDt DESC LIMIT 20001",hospital,start.atStartOfDay(),end.atTime(23,59,59));if(rows.size()>20000)throw new BusinessException(409,"本期超过原单次20000条范围，请按期间拆分，避免漏出账单");
  var report=template("report",reportTemplate);var mail=template("mail",mailTemplate);String reportText=Objects.toString(report.get("model_content"),""),mailText=Objects.toString(mail.get("model_content"),"");String payUrl=null;boolean configured=true;try{payUrl=links.hospital(hospital);}catch(BusinessException e){if(e.getCode()!=409)throw e;configured=false;}
  var result=new LinkedHashMap<String,Object>();result.put("hospitalId",hospital);result.put("month",period);result.put("title",period+Objects.toString(hosp.get("HospitalName"),"")+"德国纳博科临实验室账单");result.put("pdfTitle",result.get("title"));result.put("unpaid",LegacyMonthlyReportRules.unpaid(rows).toPlainString());result.put("count",rows.size());result.put("items",rows);result.put("reportTemplateId",report.get("id"));result.put("mailTemplateId",mail.get("id"));result.put("paymentLinkConfigured",configured);
  LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));result.put("reportHtml",LegacyMonthlyReportRules.render(reportText,Objects.toString(hosp.get("HospitalName"),""),hospital,start,end,rows,payUrl,today));result.put("mailHtml",LegacyMonthlyReportRules.render(mailText,Objects.toString(hosp.get("HospitalName"),""),hospital,start,end,rows,payUrl,today));result.put("paymentLinkRequiredButMissing",!configured&&(LegacyDictionarySelector.display(reportText).contains("{用户付款链接}")||LegacyDictionarySelector.display(mailText).contains("{用户付款链接}")));result.put("sendEnabled",false);return result;
 }
}
