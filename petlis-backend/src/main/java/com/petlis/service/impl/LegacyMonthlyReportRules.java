package com.petlis.service.impl;
import java.math.*;
import java.time.*;
import java.util.*;
/** Hospmonthpeport.contents/html and common.php numToRmb, not a new financial formula. */
final class LegacyMonthlyReportRules {
 private LegacyMonthlyReportRules(){}
 static String upperMoney(BigDecimal value){
  BigInteger number=value.setScale(2,RoundingMode.HALF_UP).movePointRight(2).toBigIntegerExact();if(number.toString().length()>10)return "金额太大，请检查";
  String digits="零壹贰叁肆伍陆柒捌玖",units="分角元拾佰仟万拾佰仟亿";String result="";int i=0;
  do{int n=i==0?Character.digit(number.toString().charAt(number.toString().length()-1),10):number.remainder(BigInteger.TEN).intValue();char digit=digits.charAt(n<0?digits.length()+n:n),unit=units.charAt(i);result=""+digit+(n!=0||unit=='亿'||unit=='万'||unit=='元'?String.valueOf(unit):"")+result;number=number.divide(BigInteger.TEN);i++;}while(number.signum()!=0);
  for(int j=0;j<result.length()-1;j++){String pair=result.substring(j,j+2);if(Set.of("零元","零万","零亿","零零").contains(pair)){result=result.substring(0,j)+result.substring(j+1);j--;}}
  if(result.endsWith("零"))result=result.substring(0,result.length()-1);return result.isEmpty()?"零元整":result+"整";
 }
 static BigDecimal unpaid(List<Map<String,Object>> rows){BigDecimal sum=BigDecimal.ZERO.setScale(2);for(var row:rows)if(integer(row.get("IsCheckPass"))==0&&integer(row.get("PayStatus2"))==0)sum=sum.add(new BigDecimal(Objects.toString(row.get("TestPrice"),"0"))).setScale(2,RoundingMode.DOWN);return sum;}
 private static int integer(Object value){return value==null?0:((Number)value).intValue();}
 private static String text(Map<String,Object> row,String name){return Objects.toString(row.get(name),"");}
 private static String escape(String text){return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
 private static String date(Object value){if(value==null||value.toString().isEmpty())return "1970-01-01";return value.toString().replace('T',' ').substring(0,10);}
 static String table(List<Map<String,Object>> rows){
  StringBuilder out=new StringBuilder("<style>.tablegrid{border-collapse:collapse;border:2px solid #00000B;background:#fff}.tablegrid th{border:2px solid #00000B;height:25px;padding:0;color:#9CB9E0}.tablegrid td{border:2px solid #00000B;height:28px;padding:1px 3px}.viewtr1{background-color:#B8CCE4}</style><table class=\"tablegrid\" style=\"width:100%\"><tr>");
  String[] titles={"样本编号","德国编号","物种","宠物姓名","畜主姓名","检测项目","付款状态","检测费","收到样本日期","报告发出日期"};for(String title:titles)out.append("<th>").append(title).append("</th>");out.append("</tr>");
  for(var row:rows){out.append("<tr class=\"\">");String[] values={text(row,"UserNO"),text(row,"ReportNO"),text(row,"AnimalIdName"),text(row,"AnimalName"),text(row,"MasterName"),text(row,"TestName"),LegacyFinanceService.payLabel(integer(row.get("IsCheckPass")),integer(row.get("PayStatus2"))),"￥"+new BigDecimal(Objects.toString(row.get("TestPrice"),"0")).setScale(2,RoundingMode.HALF_UP).toPlainString(),date(row.get("ReceiveSampleDt")),date(row.get("SendMailDt"))};for(String value:values)out.append("<td>").append(escape(value)).append("</td>");out.append("</tr>");}return out.append("</table>").toString();
 }
 static String render(String template,String hospital,long hospitalId,LocalDate start,LocalDate end,List<Map<String,Object>> rows,String payUrl,LocalDate today){
  String html=LegacyDictionarySelector.display(template);BigDecimal sum=unpaid(rows);String result=html.replace("{结算单位}",hospital).replace("{结算月份}",String.format(Locale.ROOT,"%04d年%02d月",start.getYear(),start.getMonthValue())).replace("{起始时间}",start.toString()).replace("{结束时间}",end.toString()).replace("{当前时间}",today.toString()).replace("{总费用}",sum.toPlainString()).replace("{总费用大写}",upperMoney(sum)).replace("{表格内容}",table(rows));return payUrl==null?result:result.replace("{用户付款链接}",payUrl);
 }
}
