package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import java.util.*;
import java.util.regex.Pattern;

/** Flat species/sex selectpage contract; identifiers are supplied only by the server dictionary definition. */
final class LegacyDictionarySelector {
 private LegacyDictionarySelector(){}
 record Query(List<String> words,List<String> searchFields,String andOr,List<Long> selectedIds,int page,int size,Integer active){}
 record Plan(String where,List<Object> args,List<Long> selectedIds,int offset,int limit){}
 static Plan compile(String id,String name,String englishName,Query q){return compile(id,name,englishName,q,4294967295L);}
 static Plan compile(String id,String name,String englishName,Query q,long maxId){
  if(q==null)throw invalid("选择条件缺失");
  if(q.selectedIds()!=null){
   var ids=new ArrayList<Long>(new LinkedHashSet<>(q.selectedIds()));
   if(ids.stream().anyMatch(v->v==null||v<1||v>maxId))throw invalid("字典编号无效");
   return new Plan(ids.isEmpty()?" WHERE 1=0":" WHERE "+id+" IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")",new ArrayList<>(ids),List.copyOf(ids),0,999999);
  }
  if(q.page()<1||q.size()<1||q.size()>100||((long)q.page()-1)*q.size()>Integer.MAX_VALUE)throw invalid("分页无效");
  if(q.active()!=null&&q.active()!=0&&q.active()!=1)throw invalid("状态无效");
  var fields=q.searchFields()==null||q.searchFields().isEmpty()?List.of(name):q.searchFields();
  if(fields.stream().anyMatch(f->!new java.util.HashSet<>(java.util.List.of(id,name,englishName,"IsActive","createtime","updatetime")).contains(f)))throw invalid("搜索字段无效");
  var words=new LinkedHashSet<String>();
  if(q.words()!=null)for(String word:q.words()){String clean=filter(word);if(!clean.isEmpty()&&!clean.equals("0"))words.add(clean);}
  var clauses=new ArrayList<String>();var args=new ArrayList<Object>();
  String logic="AND".equalsIgnoreCase(q.andOr()==null?"AND":q.andOr())?" AND ":" OR ";
  for(String word:words){var parts=new ArrayList<String>();for(String field:fields){parts.add(field+" LIKE ?");args.add("%"+word+"%");}clauses.add("("+String.join(logic,parts)+")");}
  String where=clauses.isEmpty()?" WHERE 1=1":" WHERE ("+String.join(" OR ",clauses)+")";
  if(q.active()!=null){where+=" AND IsActive=?";args.add(q.active());}
  return new Plan(where,List.copyOf(args),null,(q.page()-1)*q.size(),q.size());
 }
 // The request uses trim, strip_tags, htmlspecialchars(ENT_COMPAT). Complex malformed PHP tags remain a parity review case.
 private static String filter(String value){if(value==null)return "";return value.trim().replaceAll("(?s)<[^>]*>","").replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;");}
 private static final Pattern UNICODE=Pattern.compile("\\\\u([0-9a-f]{1,4})",Pattern.CASE_INSENSITIVE);
 static String display(Object value){
  if(value==null)return "";var matcher=UNICODE.matcher(value.toString());var result=new StringBuffer();
  while(matcher.find()){int code=Integer.parseInt(matcher.group(1),16);String replacement=code>=0xd800&&code<=0xdfff?"\ufffd":String.valueOf((char)code);matcher.appendReplacement(result,java.util.regex.Matcher.quoteReplacement(replacement));}matcher.appendTail(result);
  return result.toString().replace("http://vetlab.vettina.com/","http://my.laboklin.cn/")
   .replace("style=\"font-family: \" microsoft=\"\" yahei\";=\"\" font-size:=\"\" 16px;\"=\"\"","style=\"font-family:Microsoft YaHei;font-size:16px\"")
   .replace("style=\"font-family: \" microsoft=\"\" yahei\";\"=\"","style=\"font-family:Microsoft YaHei;")
   .replace("\"Microsoft YaHei\";","Microsoft YaHei;");
 }
 private static BusinessException invalid(String message){return new BusinessException(400,message);}
}
