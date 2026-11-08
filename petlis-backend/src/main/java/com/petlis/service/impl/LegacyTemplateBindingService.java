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
/** Original lb_dictbindkey maintenance; binding strings are data, never SQL identifiers. */
@Service @RequiredArgsConstructor
public class LegacyTemplateBindingService {
 private final FeatureStore store;private final AccessService access;
 public record Command(String bindTable,String columnName,String description,String requestKey,String expectedToken){}
 public record DeleteCommand(String requestKey,String expectedToken){}
 private boolean[] policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-template-bindings' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));if(rows.size()!=1)return new boolean[]{false,false,false};
  try{var j=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=j.path("enabled").isBoolean()&&j.path("enabled").asBoolean(),write=read&&j.path("writeEnabled").isBoolean()&&j.path("writeEnabled").asBoolean();return new boolean[]{read,write,write&&j.path("deleteEnabled").isBoolean()&&j.path("deleteEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"变量绑定参数无效");}
 }
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("templateBindings",p[0],"bindingWrites",p[1],"bindingDeletes",p[2]);}
 private void read(){if(!policy(false)[0])throw new BusinessException(409,"变量绑定维护尚未开启");}
 private void id(long id){if(id<1||id>4294967295L)throw new BusinessException(400,"变量绑定编号无效");}
 private Map<String,Object> raw(long id,boolean lock){id(id);return new LinkedHashMap<>(store.one("SELECT id,BindTable,ColumnName,DescName FROM lin_lb_dictbindkey WHERE id=?"+(lock?" FOR UPDATE":""),id));}
 private String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @Transactional(readOnly=true) public Map<String,Object> list(String q,int page,int size){read();if(page<0||size<1||size>100||q!=null&&q.length()>1000)throw new BusinessException(400,"变量绑定查询无效");String term="%"+(q==null?"":q)+"%",where=" FROM lin_lb_dictbindkey WHERE BindTable LIKE ? OR ColumnName LIKE ? OR DescName LIKE ?";return store.page("SELECT COUNT(*)"+where,"SELECT id,BindTable,ColumnName,DescName"+where+" ORDER BY id DESC",page,size,term,term,term);}
 @Transactional(readOnly=true) public Map<String,Object> detail(long id){read();var row=raw(id,false);row.put("editToken",token(row));return row;}
 private void request(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"变量绑定请求号无效");}
 private void version(String token){if(token==null||!token.matches("[a-f0-9]{64}"))throw new BusinessException(400,"请重新打开绑定记录后修改");}
 private Long replay(int actor,String key,String canonical){var rows=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_TEMPLATE_BINDING' AND request_id=? ORDER BY id DESC LIMIT 1",actor,key);if(rows.isEmpty())return null;try{var j=new ObjectMapper().readTree(rows.get(0).get("after_json").toString());if(!canonical.equals(j.path("canonicalRequest").asText()))throw new BusinessException(409,"请求号已用于其他绑定操作");return j.path("bindingId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"变量绑定请求无法核对");}}
 private void audit(int actor,String action,long id,String key,Map<String,Object> before,String canonical){store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,?,'LEGACY_TEMPLATE_BINDING',?,?,?,?)",actor,action,String.valueOf(id),key,before==null?null:store.encode(before),store.encode(Map.of("bindingId",id,"canonicalRequest",canonical)));}
 @Transactional public long save(Long id,Command c){
  if(!policy(true)[1])throw new BusinessException(409,"变量绑定写入尚未开启");if(c==null)throw new BusinessException(400,"变量绑定字段缺失");
  for(String value:new String[]{c.bindTable(),c.columnName(),c.description()})if(value==null||value.isBlank()||value.codePointCount(0,value.length())>50)throw new BusinessException(400,"绑定表、绑定列和变量名称必填且不超过50字");
  request(c.requestKey());if(id!=null)version(c.expectedToken());int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String canonical=store.encode(Map.of("operation",id==null?"CREATE":"UPDATE","id",id==null?0:id,"command",c));var prior=replay(actor,c.requestKey(),canonical);if(prior!=null)return prior;
  Map<String,Object> before=null;long result;
  if(id==null)result=store.insert("INSERT INTO lin_lb_dictbindkey(BindTable,ColumnName,DescName) VALUES(?,?,?)",c.bindTable(),c.columnName(),c.description());
  else{before=raw(id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"绑定记录已变更，请重新打开核对");result=id;store.jdbc().update("UPDATE lin_lb_dictbindkey SET BindTable=?,ColumnName=?,DescName=? WHERE id=?",c.bindTable(),c.columnName(),c.description(),id);}
  audit(actor,id==null?"LEGACY_BINDING_CREATED":"LEGACY_BINDING_UPDATED",result,c.requestKey(),before,canonical);return result;
 }
 @Transactional public long delete(long id,DeleteCommand c){
  if(!policy(true)[2])throw new BusinessException(409,"变量绑定删除尚未开启");id(id);if(c==null)throw new BusinessException(400,"删除请求无效");request(c.requestKey());version(c.expectedToken());int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  String canonical=store.encode(Map.of("operation","DELETE","id",id,"command",c));var prior=replay(actor,c.requestKey(),canonical);if(prior!=null)return prior;
  var before=raw(id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"绑定记录已变更，请重新打开核对");store.jdbc().update("DELETE FROM lin_lb_dictbindkey WHERE id=?",id);audit(actor,"LEGACY_BINDING_DELETED",id,c.requestKey(),before,canonical);return id;
 }
}
