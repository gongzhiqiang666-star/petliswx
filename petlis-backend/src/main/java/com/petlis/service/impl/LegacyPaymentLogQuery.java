package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.*;
import java.util.*;
/** Fixed business columns and bound values. No client SQL fragments are accepted. */
final class LegacyPaymentLogQuery {
 static final List<String> TEXT=List.of("BuyMail","BuyerId","TradeNoAli","TradeNoUser","TradeStatus","Subject","TradeBody","Exterface");
 record Plan(String where,List<Object> args,String order){}
 static Plan build(Map<String,String> input){
  Map<String,String> q=input==null?Map.of():input;var allowed=new HashSet<>(TEXT);allowed.addAll(List.of("PayLogId","q","page","size","sort","order","amountMin","amountMax","from","to"));if(!allowed.containsAll(q.keySet()))throw bad("未知流水查询字段");
  StringBuilder where=new StringBuilder(" FROM lin_lb_paylog WHERE 1=1");var args=new ArrayList<Object>();String search=get(q,"q",1000);
  if(!search.isEmpty()){where.append(" AND (TradeNoAli LIKE ? OR TradeNoUser LIKE ? OR BuyerId LIKE ? OR Subject LIKE ?)");for(int i=0;i<4;i++)args.add("%"+search+"%");}
  for(String field:TEXT){String value=get(q,field,1000).trim();if(value.isEmpty())continue;
   if(value.equalsIgnoreCase("NULL")||value.equalsIgnoreCase("NOT NULL")){where.append(" AND ").append(field).append(value.equalsIgnoreCase("NULL")?" IS NULL":" IS NOT NULL");}
   else if(value.equals("\"\"")||value.equals("''")){where.append(" AND ").append(field).append("=?");args.add("");}
   else{where.append(" AND ").append(field).append(" LIKE ?");args.add("%"+value+"%");}
  }
  String id=get(q,"PayLogId",10).trim();if(!id.isEmpty()){if(!id.matches("[0-9]{1,10}")||Long.parseLong(id)>4294967295L)throw bad("流水编号条件无效");where.append(" AND PayLogId=?");args.add(Long.parseLong(id));}
  String min=get(q,"amountMin",100).trim(),max=get(q,"amountMax",100).trim();
  // PHP array_filter considers literal "0" empty. "0.00" is a real zero bound.
  if(!((min.isEmpty()||min.equals("0"))&&(max.isEmpty()||max.equals("0")))){
   BigDecimal low=min.isEmpty()?null:money(min),high=max.isEmpty()?null:money(max);if(low!=null&&high!=null&&low.compareTo(high)>0)throw bad("最小金额不能大于最大金额");
   if(low!=null){where.append(" AND TotalFee>=?");args.add(low);}if(high!=null){where.append(" AND TotalFee<=?");args.add(high);}
  }
  String from=get(q,"from",19).trim(),to=get(q,"to",19).trim();LocalDateTime low=from.isEmpty()?null:date(from),high=to.isEmpty()?null:date(to);if(low!=null&&high!=null&&low.isAfter(high))throw bad("开始时间不能晚于结束时间");
  if(low!=null){where.append(" AND CreateDt>=?");args.add(low);}if(high!=null){where.append(" AND CreateDt<=?");args.add(high);}
  String sort=get(q,"sort",250).trim(),direction=get(q,"order",4).trim().toUpperCase(Locale.ROOT);if(sort.isEmpty())sort="PayLogId";if(direction.isEmpty())direction="DESC";if(!List.of("ASC","DESC").contains(direction))throw bad("排序方向无效");
  var fields=new LinkedHashSet<String>();for(String field:sort.split(",",-1)){field=field.trim();if(!field.equals("PayLogId")&&!LegacyPaymentLogService.FIELDS.contains(field))throw bad("排序字段无效");fields.add(field);}fields.add("PayLogId");var clauses=new ArrayList<String>();for(String field:fields)clauses.add(field+" "+direction);
  return new Plan(where.toString(),List.copyOf(args)," ORDER BY "+String.join(",",clauses));
 }
 private static String get(Map<String,String> q,String key,int length){String v=q.get(key);if(v==null)return "";if(v.length()>length)throw bad("流水查询条件过长");return v;}
 private static BigDecimal money(String value){if(!value.matches("[+-]?[0-9]{1,18}(\\.[0-9]{1,8})?"))throw bad("金额条件须为十进制数");return new BigDecimal(value);}
 private static LocalDateTime date(String value){try{var date=LocalDateTime.parse(value,DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT));if(date.getYear()<1000||date.getYear()>9999)throw new IllegalArgumentException();return date;}catch(Exception e){throw bad("交易时间条件须为有效的年月日时分秒");}}
 private static BusinessException bad(String message){return new BusinessException(400,message);}
}
