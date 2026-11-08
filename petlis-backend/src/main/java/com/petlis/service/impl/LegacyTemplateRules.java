package com.petlis.service.impl;
import java.util.Map;
import java.util.List;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import java.util.Objects;
/** Pure legacy template helper rules, before any editor or HTML rendering. */
final class LegacyTemplateRules {
 private LegacyTemplateRules(){}
 static String mailTitle(Map<String,Object> project,String supplied){
  String title=supplied;
  // PHP empty: the string "0" is empty, while whitespace and "0.0" are not.
  if(title==null||title.isEmpty()||title.equals("0"))title=value(project,"InfoAnimalAnimalName")+value(project,"AnimalType")+value(project,"AnimalName")+"（"+value(project,"MasterName")+"）（"+value(project,"UserNO")+"）的检测报告";
  int start=0,end=title.length();while(start<end&&title.charAt(start)==',')start++;while(end>start&&title.charAt(end-1)==',')end--;
  return title.substring(start,end);
 }
 record Binding(String table,String column,String description){}
 private static final List<String> TABLES=List.of("ListDiagnosisProject","InfoHospital","DictSex","InfoAnimal","InfoSampleType","VwDiagnosisDetail");
 private static final Pattern DATE=Pattern.compile("^(\\d{4})([-/])(\\d{2})\\2(\\d{2})[ Tt](\\d{2}):(\\d{2}):(\\d{2})$");
 private static final Pattern ALT_DATE=Pattern.compile("^(\\d{2})([./-])(\\d{2})\\2(\\d{4})[ Tt](\\d{2}):(\\d{2}):(\\d{2})$");
 static String render(String body,List<Binding> bindings,Map<String,Map<String,Object>> rows,String url,LocalDate today,String diagnosticNames,int type){
  String contents=LegacyDictionarySelector.display(body);
  String pay="<a target='_blank' href='"+url+"'>"+url+"</a>";
  String view="<br/><br/><span style='color:red'>附件-报告地址:<a target='_blank' href='"+url+"'>"+url+"</a></span>";
  // PHP strpos returns 0 at the start, which is false in the original conditional.
  if(contents.indexOf("{报告链接}")>0)contents=contents.replace("{报告链接}",view);
  if(type==1&&contents.indexOf("{用户付款链接}")>0)contents=contents.replace("{用户付款链接}",pay);
  contents=contents.replace("----分页符号----","").replace("{当前时间}",today.toString());
  for(String table:TABLES){var row=rows.get(table);if(row==null||row.isEmpty())continue;
   for(var binding:bindings)if(table.equals(binding.table())&&row.get(binding.column())!=null)contents=contents.replace("{"+binding.description()+"}",formatBinding(row.get(binding.column())));
  }
  if(type==1)contents+=view;
  return contents.replace("{诊断项名称}",diagnosticNames==null?"":diagnosticNames);
 }
 private static String formatBinding(Object value){
  String raw;
  if(value instanceof Boolean b)raw=b?"1":"";
  else if(value instanceof BigDecimal number)raw=number.toPlainString();
  else if(value instanceof java.sql.Timestamp timestamp)raw=timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"));
  else if(value instanceof LocalDateTime datetime)raw=datetime.format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"));
  else raw=value.toString();
  if(raw.getBytes(StandardCharsets.UTF_8).length!=19)return raw;
  var match=DATE.matcher(raw);boolean yearFirst=match.matches();if(!yearFirst){match=ALT_DATE.matcher(raw);if(!match.matches())return raw;}
  try{
   int year,month,day;
   if(yearFirst){year=Integer.parseInt(match.group(1));month=Integer.parseInt(match.group(3));day=Integer.parseInt(match.group(4));}
   else{year=Integer.parseInt(match.group(4));boolean american=match.group(2).equals("/");month=Integer.parseInt(match.group(american?1:3));day=Integer.parseInt(match.group(american?3:1));}
   int hour=Integer.parseInt(match.group(5)),minute=Integer.parseInt(match.group(6)),second=Integer.parseInt(match.group(7));
   if(month>12||day>31||hour>24||minute>59||second>60)return raw;
   var time=LocalDate.of(year,1,1).plusMonths(month-1L).plusDays(day-1L).atStartOfDay().plusHours(hour).plusMinutes(minute).plusSeconds(second);
   if(time.atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond()<=0)return raw;
   return String.format(java.util.Locale.ROOT,"%04d-%02d-%02d",time.getYear(),time.getMonthValue(),time.getDayOfMonth());
  }catch(DateTimeException|NumberFormatException invalid){return raw;}
 }
 private static String value(Map<String,Object> row,String field){return Objects.toString(row.get(field),"");}
}
