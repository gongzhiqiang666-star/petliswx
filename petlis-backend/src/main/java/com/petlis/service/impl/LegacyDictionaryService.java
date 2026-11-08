package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Original dictionary read contracts. No generic table/column names are accepted from clients. */
@Service @RequiredArgsConstructor
public class LegacyDictionaryService {
 private final FeatureStore store;
 private final AccessService access;
 @org.springframework.beans.factory.annotation.Autowired private org.springframework.cache.CacheManager cacheManager;
 private void invalidateDictionaryCacheAfterCommit(){
  if(cacheManager==null)return;
  Runnable invalidate=()->{var cache=cacheManager.getCache("dictionary");if(cache!=null)cache.clear();};
  if(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()){
   org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){@Override public void afterCommit(){invalidate.run();}});
  }else invalidate.run();
 }
 public record Target(long id,String editToken){}
 public record BatchCommand(List<Target> targets,Integer active,String requestKey){}
 public record Command(String name,String englishName,Integer active,String requestKey,String expectedToken){}
 public record Query(String name,String englishName,Integer active,Long createdFrom,Long createdTo,Long updatedFrom,Long updatedTo,String sort,String order,int page,int size){}
 private record Definition(String kind,String table,String id,String name,String englishName,String label,String nameLabel,int maxLength){boolean hasStatus(){return !kind.equals("variety");}long maxId(){return hasStatus()?4294967295L:Long.MAX_VALUE;}}
 private static final List<Definition> DEFINITIONS=List.of(
  new Definition("species","lin_lb_infoanimal","InfoAnimalId","AnimalName","AnimalName_en","物种","物种名称",250),
  new Definition("sex","lin_lb_dictsex","DictSexId","SexName","SexName_en","性别","性别名称",50),
  new Definition("sampleType","lin_lb_infosampletype","InfoSampleTypeId","SampleName","SampleName_en","采样类型","采样类型",250),
  new Definition("variety","lin_lb_infovariety","IID","Variety_CN","Variety_EN","品种","中文名称",50));

 private record Policy(boolean enabled,boolean canWrite,boolean canDelete){}
 @Transactional(readOnly=true) public boolean enabled(){return policy(false).enabled();}
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("dictionaries",p.enabled(),"dictionaryWrites",p.canWrite(),"dictionaryDeletes",p.canDelete());}
 private Policy policy(boolean lock){
  access.requireAdmin();
  var rows=store.jdbc().queryForList("SELECT JSON_TYPE(JSON_EXTRACT(config_value,'$.enabled')) AS enabledType,JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) AS enabled,JSON_TYPE(JSON_EXTRACT(config_value,'$.writeEnabled')) AS writeType,JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.writeEnabled')) AS writeEnabled,JSON_TYPE(JSON_EXTRACT(config_value,'$.deleteEnabled')) AS deleteType,JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.deleteEnabled')) AS deleteEnabled FROM lin_operational_config WHERE config_key=? AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR SHARE":""),"legacy-dictionaries");
  if(rows.size()!=1)return new Policy(false,false,false);var r=rows.get(0);
  boolean read="BOOLEAN".equals(r.get("enabledType"))&&"true".equals(r.get("enabled"));
  boolean write=read&&"BOOLEAN".equals(r.get("writeType"))&&"true".equals(r.get("writeEnabled"));
  return new Policy(read,write,write&&"BOOLEAN".equals(r.get("deleteType"))&&"true".equals(r.get("deleteEnabled")));
 }
 private void requireEnabled(){if(!enabled())throw new BusinessException(409,"基础字典迁移模块尚未开启");}
 @Transactional(readOnly=true)
 public List<Map<String,Object>> schemas(){
  requireEnabled();var policy=policy(false);boolean canWrite=policy.canWrite();var result=new ArrayList<Map<String,Object>>();
  for(var d:DEFINITIONS){
   var columns=new ArrayList<Map<String,String>>();columns.add(Map.of("key",d.id(),"label","Id"));columns.add(Map.of("key",d.name(),"label",d.nameLabel()));columns.add(Map.of("key",d.englishName(),"label",d.hasStatus()?d.nameLabel()+"(英文)":"英文名称"));
   if(d.hasStatus())columns.add(Map.of("key","IsActive","label","状态"));
   columns.add(Map.of("key","createtime","label",d.kind().equals("sampleType")||!d.hasStatus()?"添加时间":"创建时间"));columns.add(Map.of("key","updatetime","label","更新时间"));
   var schema=new LinkedHashMap<String,Object>();schema.put("kind",d.kind());schema.put("label",d.label());schema.put("columns",columns);schema.put("hasStatus",d.hasStatus());schema.put("canWrite",canWrite);schema.put("canDelete",policy.canDelete());schema.put("maxNameLength",d.maxLength());
   if(d.hasStatus()){schema.put("createDefaultIsActive",0);schema.put("statusLabels",Map.of("0","禁用","1","正常"));}result.add(schema);
  }
  return result;
 }
 @Transactional(readOnly=true)
 public Map<String,Object> list(String kind,Query query){
  requireEnabled();var d=definition(kind);
  if(query==null)throw invalid("查询条件缺失");
  String sort=query.sort()==null?d.id():query.sort(),order=query.order()==null?"DESC":query.order();
  if(!Arrays.asList(fields(d).split(",")).contains(sort) || !Set.of("ASC","DESC").contains(order))throw invalid("排序字段或方向无效");
  if(query.active()!=null && (!d.hasStatus() || query.active()!=0 && query.active()!=1))throw invalid("状态无效");
  if(query.page()<0 || query.size()<1 || query.size()>100)throw invalid("分页无效");
  var args=new ArrayList<Object>();var where=new StringBuilder(" FROM "+d.table()+" WHERE 1=1");
  nameFilter(where,args,d.name(),query.name());nameFilter(where,args,d.englishName(),query.englishName());
  if(query.active()!=null){where.append(" AND IsActive=?");args.add(query.active());}
  timeFilter(where,args,"createtime",query.createdFrom(),query.createdTo());timeFilter(where,args,"updatetime",query.updatedFrom(),query.updatedTo());
  String fields=fields(d);
  String ordering=" ORDER BY "+sort+" "+order+(sort.equals(d.id())?"":","+d.id()+" "+order);
  return store.page("SELECT COUNT(*)"+where,"SELECT "+fields+where+ordering,query.page(),query.size(),args.toArray());
 }

 /** Read-only flat picker. Selected IDs deliberately bypass ordinary search/status filters. */
 @Transactional(readOnly=true)
 public Map<String,Object> select(String kind,List<String> words,List<String> searchFields,String andOr,List<Long> selectedIds,int page,int size,Integer active){
  requireEnabled();var d=definition(kind);
  if(!d.hasStatus() && (active!=null || searchFields!=null&&searchFields.contains("IsActive")))throw invalid("品种没有状态字段");
  var plan=LegacyDictionarySelector.compile(d.id(),d.name(),d.englishName(),new LegacyDictionarySelector.Query(words,searchFields,andOr,selectedIds,page,size,active),d.maxId());
  String from=" FROM "+d.table()+plan.where();
  List<Map<String,Object>> rows;Number total;
  if(plan.selectedIds()!=null){
   rows=store.jdbc().queryForList("SELECT "+d.id()+","+d.name()+from,plan.args().toArray());
   var order=new HashMap<Long,Integer>();for(int n=0;n<plan.selectedIds().size();n++)order.put(plan.selectedIds().get(n),n);
   rows=new ArrayList<>(rows);rows.sort(Comparator.comparingInt(r->order.get(((Number)r.get(d.id())).longValue())));total=rows.size();
  }else{
   total=store.jdbc().queryForObject("SELECT COUNT(*)"+from,Long.class,plan.args().toArray());
   var args=new ArrayList<>(plan.args());args.add(plan.limit());args.add(plan.offset());
   rows=store.jdbc().queryForList("SELECT "+d.id()+","+d.name()+from+" ORDER BY "+d.id()+" ASC LIMIT ? OFFSET ?",args.toArray());
  }
  var list=new ArrayList<Map<String,Object>>();
  for(var r:rows)list.add(Map.of(d.id(),r.get(d.id()),d.name(),LegacyDictionarySelector.display(r.get(d.name())),"pid",0));
  return Map.of("list",list,"total",total==null?0:total);
 }

 private Definition definition(String kind){return DEFINITIONS.stream().filter(x->x.kind().equals(kind)).findFirst().orElseThrow(()->invalid("未知字典类型"));}
 private String fields(Definition d){return d.id()+","+d.name()+","+d.englishName()+(d.hasStatus()?",IsActive":"")+",createtime,updatetime";}
 private Map<String,Object> row(Definition d,long id,boolean lock){
  if(id<1 || id>d.maxId())throw invalid("字典编号无效");
  return store.one("SELECT "+fields(d)+" FROM "+d.table()+" WHERE "+d.id()+"=?"+(lock?" FOR UPDATE":""),id);
 }
 private String digest(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private String token(Map<String,Object> row){return digest(store.encode(row));}
 @Transactional(readOnly=true)
 public Map<String,Object> get(String kind,long id){requireEnabled();var raw=row(definition(kind),id,false);var result=new LinkedHashMap<String,Object>(raw);result.put("editToken",token(raw));return result;}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public long create(String kind,Command command){return write(kind,null,command);}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public long update(String kind,long id,Command command){return write(kind,id,command);}
 private long write(String kind,Long id,Command c){
  if(!policy(true).canWrite())throw new BusinessException(409,"基础字典维护尚未开启");
  var d=definition(kind);validate(d,c);
  if(id!=null && (c.expectedToken()==null || !c.expectedToken().matches("[a-f0-9]{64}")))throw invalid("请重新打开字典记录后编辑");
  int actor=access.current().userId();
  // A stable actor lock makes request-key replay atomic without changing the old dictionary schema.
  store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  var payload=new LinkedHashMap<String,Object>();payload.put("kind",kind);payload.put("id",id);payload.put("name",c.name());payload.put("englishName",c.englishName());payload.put("active",d.hasStatus()&&id==null&&c.active()==null?Integer.valueOf(0):c.active());payload.put("expectedToken",id==null?null:c.expectedToken());
  String hash=digest(store.encode(payload));
  var previous=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_DICTIONARY' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!previous.isEmpty()){
   try{var json=new ObjectMapper().readTree(String.valueOf(previous.get(0).get("after_json")));if(!hash.equals(json.path("payloadHash").asText()) || !json.path("resultId").canConvertToLong())throw new BusinessException(409,"请求号已用于其他字典操作");return json.path("resultId").asLong();}
   catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"原字典请求无法校验，请核对记录");}
  }
  Map<String,Object> before=null;long resultId;
  if(id==null){
   if(d.hasStatus())resultId=store.insert("INSERT INTO "+d.table()+"("+d.name()+","+d.englishName()+",IsActive,createtime,updatetime) VALUES(?,?,?,UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",c.name(),c.englishName(),c.active()==null?0:c.active());
   else resultId=store.insert("INSERT INTO "+d.table()+"("+d.name()+","+d.englishName()+",createtime,updatetime) VALUES(?,?,UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",c.name(),c.englishName());
  }else{
   before=row(d,id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"字典记录已变更，请刷新后核对再保存");
   int active=!d.hasStatus()?0:c.active()==null?((Number)before.get("IsActive")).intValue():c.active();
   if(!Objects.equals(before.get(d.name()),c.name()) || !Objects.equals(before.get(d.englishName()),c.englishName()) || d.hasStatus() && ((Number)before.get("IsActive")).intValue()!=active){
    if(d.hasStatus())store.jdbc().update("UPDATE "+d.table()+" SET "+d.name()+"=?,"+d.englishName()+"=?,IsActive=?,updatetime=UNIX_TIMESTAMP() WHERE "+d.id()+"=?",c.name(),c.englishName(),active,id);
    else store.jdbc().update("UPDATE "+d.table()+" SET "+d.name()+"=?,"+d.englishName()+"=?,updatetime=UNIX_TIMESTAMP() WHERE "+d.id()+"=?",c.name(),c.englishName(),id);
   }
   resultId=id;
  }
  var after=new LinkedHashMap<String,Object>();after.put("record",row(d,resultId,false));after.put("payloadHash",hash);after.put("resultId",resultId);
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,?,'LEGACY_DICTIONARY',?,?,?,?)",actor,id==null?"LEGACY_DICTIONARY_CREATED":"LEGACY_DICTIONARY_UPDATED",kind+":"+resultId,c.requestKey(),before==null?null:store.encode(before),store.encode(after));
  invalidateDictionaryCacheAfterCommit();
  return resultId;
 }
 private void validate(Definition d,Command c){
  if(c==null || c.name()==null || c.name().isBlank() || c.englishName()==null || c.englishName().isBlank())throw invalid("中英文名称均不能为空");
  if(c.name().codePointCount(0,c.name().length())>d.maxLength() || c.englishName().codePointCount(0,c.englishName().length())>d.maxLength())throw invalid("名称长度超出原字段限制");
  if(c.active()!=null && (!d.hasStatus() || c.active()!=0 && c.active()!=1))throw invalid("状态无效");
  if(c.requestKey()==null || !c.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw invalid("字典请求号无效");
 }

 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public int delete(String kind,BatchCommand command){return batch(kind,command,true);}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public int status(String kind,BatchCommand command){return batch(kind,command,false);}
 private int batch(String kind,BatchCommand c,boolean deleting){
  var policy=policy(true);if(!policy.canWrite() || deleting&&!policy.canDelete())throw new BusinessException(409,deleting?"字典删除尚未开启":"基础字典维护尚未开启");
  var d=definition(kind);
  if(!deleting && !d.hasStatus())throw invalid("品种没有状态字段");
  if(c==null || c.targets()==null || c.targets().isEmpty() || c.targets().size()>1000)throw invalid("请选择1至1000条记录");
  if(c.requestKey()==null || !c.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw invalid("字典请求号无效");
  if(!deleting && (c.active()==null || c.active()!=0&&c.active()!=1) || deleting&&c.active()!=null)throw invalid("批量操作状态无效");
  var targets=new ArrayList<Target>();var ids=new HashSet<Long>();
  for(var target:c.targets()){
   if(target==null || target.id()<1 || target.id()>d.maxId() || target.editToken()==null || !target.editToken().matches("[a-f0-9]{64}") || !ids.add(target.id()))throw invalid("记录编号、快照或选择有误");targets.add(target);
  }
  targets.sort(Comparator.comparingLong(Target::id));
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
  var payload=new LinkedHashMap<String,Object>();payload.put("kind",kind);payload.put("operation",deleting?"delete":"status");payload.put("active",c.active());payload.put("targets",targets);
  String hash=digest(store.encode(payload));
  var previous=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_DICTIONARY' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!previous.isEmpty()){
   try{var json=new ObjectMapper().readTree(String.valueOf(previous.get(0).get("after_json")));if(!hash.equals(json.path("payloadHash").asText()) || !json.path("affectedCount").canConvertToInt())throw new BusinessException(409,"请求号已用于其他字典操作");return json.path("affectedCount").asInt();}
   catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"原字典请求无法校验，请核对记录");}
  }
  var before=new ArrayList<Map<String,Object>>();
  for(var target:targets){Map<String,Object> value;try{value=row(d,target.id(),true);}catch(BusinessException error){if(error.getCode()==404)throw new BusinessException(409,"所选记录已删除，请刷新核对");throw error;}if(!token(value).equals(target.editToken()))throw new BusinessException(409,"所选记录已变更，请刷新核对后操作");before.add(value);}
  int count=0;
  for(var value:before){long id=((Number)value.get(d.id())).longValue();
   if(deleting)count+=store.jdbc().update("DELETE FROM "+d.table()+" WHERE "+d.id()+"=?",id);
   else if(((Number)value.get("IsActive")).intValue()!=c.active())count+=store.jdbc().update("UPDATE "+d.table()+" SET IsActive=?,updatetime=UNIX_TIMESTAMP() WHERE "+d.id()+"=?",c.active(),id);
  }
  if(count==0)throw invalid(deleting?"未删除任何行":"未更新任何行");
  var after=new LinkedHashMap<String,Object>();after.put("payloadHash",hash);after.put("affectedCount",count);after.put("targetIds",targets.stream().map(Target::id).toList());after.put("active",c.active());
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,?,'LEGACY_DICTIONARY',?,?,?,?)",actor,deleting?"LEGACY_DICTIONARY_DELETED":"LEGACY_DICTIONARY_STATUS_CHANGED",kind+":batch",c.requestKey(),store.encode(before),store.encode(after));
  invalidateDictionaryCacheAfterCommit();
  return count;
 }
 private static void nameFilter(StringBuilder where,List<Object> args,String column,String value){
  if(value==null || value.trim().isEmpty())return;
  if(value.length()>1024)throw invalid("检索内容过长");
  where.append(" AND "+column+" LIKE ?");args.add("%"+value.trim()+"%");
 }
 private static void timeFilter(StringBuilder where,List<Object> args,String column,Long from,Long to){
  if(from!=null && from<0 || to!=null && to<0 || from!=null && to!=null && from>to)throw invalid("日期范围无效");
  if(from!=null){where.append(" AND "+column+">=?");args.add(from);}if(to!=null){where.append(" AND "+column+"<=?");args.add(to);}
 }
 private static BusinessException invalid(String message){return new BusinessException(400,message);}
}
