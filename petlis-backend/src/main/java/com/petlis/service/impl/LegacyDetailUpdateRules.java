package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import java.time.*;
import java.time.format.*;
import java.util.Map;
/** application/admin/model/LbDiagnosisdetail.php beforeUpdate and common.php GenterPdf.
 * GenterPdf constructs a filename only; it does not create a PDF file. */
final class LegacyDetailUpdateRules {
 record Effects(LocalDateTime sentAt,boolean completeParent,String pdfDateSource){}
 static Effects effects(int sent,String input,LocalDateTime now){
  String date=input==null?"":input;
  LocalDateTime parsed=date.isEmpty()?LocalDateTime.of(1970,1,1,0,0):parse(date);
  if(sent==1&&parsed.getYear()<2000){date=now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));parsed=now;}
  boolean parent=!date.isEmpty();
  return new Effects(parsed,parent,parent?date:"1970-01-01");
 }
 private static LocalDateTime parse(String input){
  if(input.equals("-"))return LocalDateTime.of(1970,1,1,0,0);
  try{if(input.matches("\\d{4}-\\d{2}-\\d{2}"))return LocalDate.parse(input).atStartOfDay();return LocalDateTime.parse(input.replace(' ','T'));}catch(DateTimeException e){throw new BusinessException(400,"发送报告日期格式无效");}
 }
 static String filename(Map<String,Object> project,Effects effect,LocalDateTime now){
  LocalDate day=effect.pdfDateSource().compareTo("1970-01-01")<=0?now.toLocalDate():effect.sentAt().toLocalDate();
  String result=day.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))+","+text(project,"AnimalName")+","+text(project,"MasterName")+","+text(project,"UserNO")+","+text(project,"ReportNO")+text(project,"DiagnosticResults")+".pdf";
  return result.replace("\"","").replace("'","").replace(" ","").replace("&","和").replace("#","");
 }
 private static String text(Map<String,Object> row,String key){Object value=row.get(key);return value==null?"":value.toString().trim();}
}
