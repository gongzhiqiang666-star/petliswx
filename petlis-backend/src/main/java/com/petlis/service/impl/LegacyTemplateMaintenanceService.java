package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
/** PHP tpl controllers/models: no body regeneration and no propagation to existing reports. */
@Service @RequiredArgsConstructor
public class LegacyTemplateMaintenanceService {
 private final FeatureStore store;private final AccessService access;
 public record Command(String title,String html,long projectId,String requestKey,String expectedToken,Integer active){public Command(String title,String html,long projectId,String requestKey,String expectedToken){this(title,html,projectId,requestKey,expectedToken,null);}}
 public record DeleteCommand(String requestKey,String expectedToken){}
 private record Schema(String table,String id,String title,String html){}
 private Schema schema(String kind){return switch(kind){case "report"->new Schema("lin_lb_infotesttypemodel","InfoTestTypeModelId","ModelName","ModelInfo");case "mail"->new Schema("lin_lb_dictmaildiagnosisreport","id","model_type","model_content");case "financeReport"->new Schema("lin_lb_dictfinancereport","id","model_name","model_content");case "financeMail"->new Schema("lin_lb_dictfinancemailreport","id","model_name","model_content");default->throw new BusinessException(400,"未知模板类型");};}
 private boolean[] policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-templates' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));
  if(rows.size()!=1)return new boolean[]{false,false,false};
  try{var j=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=j.path("enabled").isBoolean()&&j.path("enabled").asBoolean(),write=read&&j.path("writeEnabled").isBoolean()&&j.path("writeEnabled").asBoolean();return new boolean[]{read,write,write&&j.path("deleteEnabled").isBoolean()&&j.path("deleteEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"模板维护参数无效");}
 }
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("templates",p[0],"templateWrites",p[1],"templateDeletes",p[2]);}
 @Transactional(readOnly=true) public Map<String,Object> projects(String q,int page,int size){
  read();if(page<0||size<1||size>100||q!=null&&q.length()>1000)throw new BusinessException(400,"项目查询无效");String term="%"+(q==null?"":q)+"%";
  var result=store.page("SELECT COUNT(*) FROM lin_lb_infotesttype WHERE TestName LIKE ? OR TestNO LIKE ?","SELECT CAST(InfoTestTypeId AS CHAR) AS id,TestName AS name,TestNO FROM lin_lb_infotesttype WHERE TestName LIKE ? OR TestNO LIKE ? ORDER BY InfoTestTypeId",page,size,term,term);
  @SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)result.get("items");for(var row:items)row.put("name",Objects.toString(row.get("TestNO"),"")+" "+LegacyDictionarySelector.display(row.get("name")));return result;
 }
 private void read(){if(!policy(false)[0])throw new BusinessException(409,"模板维护尚未开启");}
 private void id(long id){if(id<1||id>4294967295L)throw new BusinessException(400,"模板编号无效");}
 private Map<String,Object> raw(Schema s,long id,boolean lock){id(id);return new LinkedHashMap<>(store.one("SELECT * FROM "+s.table()+" WHERE "+s.id()+"=?"+(lock?" FOR UPDATE":""),id));}
 private String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @Transactional(readOnly=true) public Map<String,Object> list(String kind,Long projectId,String q,int page,int size){
  read();var s=schema(kind);if(page<0||size<1||size>100||q!=null&&q.length()>1000||projectId!=null&&(projectId<0||projectId>Integer.MAX_VALUE))throw new BusinessException(400,"模板查询无效");
  var args=new ArrayList<Object>();args.add("%"+(q==null?"":q)+"%");String where=" WHERE "+s.title()+" LIKE ?";
  if(kind.equals("report")&&projectId!=null){where+=" AND InfoTestTypeId=?";args.add(projectId);}
  var result=store.page("SELECT COUNT(*) FROM "+s.table()+where,"SELECT "+s.id()+" AS id,"+s.title()+" AS title"+(kind.equals("report")?",InfoTestTypeId AS projectId,CreateDt AS createdAt":kind.equals("mail")?",createtime AS createdAt":kind.equals("financeMail")?",IsActive AS active":"")+" FROM "+s.table()+where+" ORDER BY "+s.id()+" DESC",page,size,args.toArray());
  @SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)result.get("items");for(var row:items)row.put("title",LegacyDictionarySelector.display(row.get("title")));return result;
 }
 @Transactional(readOnly=true) public Map<String,Object> detail(String kind,long id){
  read();var s=schema(kind);var row=raw(s,id,false);var result=new LinkedHashMap<String,Object>();result.put("id",id);result.put("title",LegacyDictionarySelector.display(row.get(s.title())));result.put("html",LegacyDictionarySelector.display(row.get(s.html())));result.put("rawTitle",row.get(s.title()));result.put("rawHtml",row.get(s.html()));result.put("projectId",kind.equals("report")?row.get("InfoTestTypeId"):0);result.put("editToken",token(row));if(kind.equals("financeMail"))result.put("active",row.get("IsActive"));return result;
 }
 private void request(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"模板请求号无效");}
 private void version(String value){if(value==null||!value.matches("[a-f0-9]{64}"))throw new BusinessException(400,"请重新打开模板后修改");}
 private Long replay(int actor,String key,String canonical){
  var rows=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_TEMPLATE' AND request_id=? ORDER BY id DESC LIMIT 1",actor,key);
  if(rows.isEmpty())return null;try{var j=new ObjectMapper().readTree(rows.get(0).get("after_json").toString());if(!new ObjectMapper().readTree(canonical).equals(new ObjectMapper().readTree(j.path("canonicalRequest").asText())))throw new BusinessException(409,"请求号已用于其他模板操作");return j.path("templateId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"模板请求无法核对");}
 }
 private void audit(int actor,String action,long id,String key,Map<String,Object> before,String canonical){store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,?,'LEGACY_TEMPLATE',?,?,?,?)",actor,action,String.valueOf(id),key,before==null?null:store.encode(before),store.encode(Map.of("templateId",id,"canonicalRequest",canonical)));}
 @Transactional public long save(String kind,Long id,Command c){
  if(!policy(true)[1])throw new BusinessException(409,"模板写入尚未开启");var s=schema(kind);
  if(c==null||c.title()==null||c.title().isBlank()||c.title().codePointCount(0,c.title().length())>1000||c.html()==null||!kind.equals("mail")&&c.html().isBlank()||c.projectId()<0||c.projectId()>Integer.MAX_VALUE||!kind.equals("report")&&c.projectId()!=0||c.active()!=null&&(!kind.equals("financeMail")||c.active()!=0&&c.active()!=1))throw new BusinessException(400,"模板名称、正文或项目无效");
  request(c.requestKey());if(id!=null)version(c.expectedToken());int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  var payload=new LinkedHashMap<String,Object>();payload.put("title",c.title());payload.put("html",c.html());payload.put("projectId",c.projectId());payload.put("requestKey",c.requestKey());payload.put("expectedToken",c.expectedToken());if(c.active()!=null)payload.put("active",c.active());
  String canonical=store.encode(Map.of("operation",id==null?"CREATE":"UPDATE","kind",kind,"id",id==null?0:id,"command",payload));var prior=replay(actor,c.requestKey(),canonical);if(prior!=null)return prior;
  Map<String,Object> before=null;long result;
  if(id==null){
   if(kind.equals("report"))result=store.insert("INSERT INTO lin_lb_infotesttypemodel(ModelName,ModelInfo,InfoTestTypeId,CreateDt) VALUES(?,?,?,?)",c.title(),c.html(),c.projectId(),LocalDateTime.now(ZoneId.of("Asia/Shanghai")).withNano(0));
   else if(kind.equals("financeReport"))result=store.insert("INSERT INTO lin_lb_dictfinancereport(model_name,model_content) VALUES(?,?)",c.title(),c.html());
   else if(kind.equals("financeMail"))result=store.insert("INSERT INTO lin_lb_dictfinancemailreport(model_name,model_content,IsActive) VALUES(?,?,?)",c.title(),c.html(),c.active()==null?0:c.active());
   else{long now=Instant.now().getEpochSecond();result=store.insert("INSERT INTO lin_lb_dictmaildiagnosisreport(model_type,model_content,createtime,updatetime) VALUES(?,?,?,?)",c.title(),c.html(),now,now);}
  }else{
   before=raw(s,id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"模板已变更，请重新打开核对");result=id;
   if(kind.equals("report"))store.jdbc().update("UPDATE lin_lb_infotesttypemodel SET ModelName=?,ModelInfo=?,InfoTestTypeId=? WHERE InfoTestTypeModelId=?",c.title(),c.html(),c.projectId(),id);
   else if(kind.equals("financeReport"))store.jdbc().update("UPDATE lin_lb_dictfinancereport SET model_name=?,model_content=? WHERE id=?",c.title(),c.html(),id);
   else if(kind.equals("financeMail"))store.jdbc().update("UPDATE lin_lb_dictfinancemailreport SET model_name=?,model_content=?,IsActive=? WHERE id=?",c.title(),c.html(),c.active()==null?before.get("IsActive"):c.active(),id);
   else store.jdbc().update("UPDATE lin_lb_dictmaildiagnosisreport SET model_type=?,model_content=?,updatetime=? WHERE id=?",c.title(),c.html(),Instant.now().getEpochSecond(),id);
  }
  audit(actor,id==null?"LEGACY_TEMPLATE_CREATED":"LEGACY_TEMPLATE_UPDATED",result,c.requestKey(),before,canonical);return result;
 }
 @Transactional public long delete(String kind,long id,DeleteCommand c){
  if(!policy(true)[2])throw new BusinessException(409,"模板删除尚未开启");var s=schema(kind);id(id);if(c==null)throw new BusinessException(400,"删除请求无效");request(c.requestKey());version(c.expectedToken());int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String canonical=store.encode(Map.of("operation","DELETE","kind",kind,"id",id,"command",c));var prior=replay(actor,c.requestKey(),canonical);if(prior!=null)return prior;
  var before=raw(s,id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"模板已变更，请重新打开核对");
  store.jdbc().update("DELETE FROM "+s.table()+" WHERE "+s.id()+"=?",id);audit(actor,"LEGACY_TEMPLATE_DELETED",id,c.requestKey(),before,canonical);return id;
 }
}
