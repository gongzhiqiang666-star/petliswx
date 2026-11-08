package com.petlis.service.impl;
import com.fasterxml.jackson.databind.*;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

/** Original global test catalogue, separate from new laboratory-specific pricing. */
@Service @RequiredArgsConstructor
public class LegacyTestCatalogService {
 private final FeatureStore store;private final AccessService access;
 @Autowired private CacheManager cacheManager;
 public record QuoteCommand(String price,String discount){}
 public record ItemCommand(String name,String code,Integer type,Integer active,String requestKey,String expectedToken){}
 public record PriceCommand(BigDecimal price,Integer active,String requestKey){}
 public record PriceEditCommand(BigDecimal price,Integer active,String requestKey,String expectedToken){}
 public record PriceDeleteCommand(String requestKey,String expectedToken){}
 private static final String FIELDS="InfoTestTypeId,TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive,createtime,updatetime";
 private record Policy(boolean enabled,boolean writable,boolean deletable){}
 private Policy policy(boolean lock){
  access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-test-catalog' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR SHARE":""));
  if(rows.size()!=1)return new Policy(false,false,false);
  try{var json=new ObjectMapper().readTree(String.valueOf(rows.get(0).get("config_value")));boolean enabled=json.path("enabled").isBoolean()&&json.path("enabled").asBoolean();boolean write=enabled&&json.path("writeEnabled").isBoolean()&&json.path("writeEnabled").asBoolean();return new Policy(enabled,write,write&&json.path("deleteEnabled").isBoolean()&&json.path("deleteEnabled").asBoolean());}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw error(409,"项目资料配置无效");}
 }
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("testCatalog",p.enabled(),"testCatalogWrites",p.writable(),"testCatalogDeletes",p.deletable());}
 @Transactional(readOnly=true) public String quote(QuoteCommand command){read();if(command==null)throw error(400,"价格或折扣格式无效");return LegacyPricingRules.quote(command.price(),command.discount());}
 private void read(){if(!policy(false).enabled())throw error(409,"原检测项目资料尚未开启");}
 private String displaySql(){return "COALESCE(REPLACE((SELECT GROUP_CONCAT(p.price) FROM lin_lb_infotesttype_price p WHERE p.InfoTestTypeid=t.InfoTestTypeId),'0000','00'),'无') AS DealPrice";}
 @Transactional(readOnly=true) public Map<String,Object> items(int page,int size){read();paging(page,size);return store.page("SELECT COUNT(*) FROM lin_lb_infotesttype","SELECT t.InfoTestTypeId,t.TestName,t.TestNO,t.type,t.IsActive,t.CreateDt,"+displaySql()+" FROM lin_lb_infotesttype t ORDER BY t.InfoTestTypeId DESC",page,size);}
 private Map<String,Object> raw(long id,boolean lock){id(id);return store.one("SELECT "+FIELDS+" FROM lin_lb_infotesttype WHERE InfoTestTypeId=?"+(lock?" FOR UPDATE":""),id);}
 @Transactional(readOnly=true) public Map<String,Object> item(long id){read();var row=new LinkedHashMap<>(raw(id,false));row.put("editToken",hash(store.encode(row)));row.put("DealPrice",store.one("SELECT "+displaySql()+" FROM lin_lb_infotesttype t WHERE t.InfoTestTypeId=?",id).get("DealPrice"));return row;}
 @Transactional(readOnly=true) public Map<String,Object> prices(long id,int page,int size){read();raw(id,false);paging(page,size);return store.page("SELECT COUNT(*) FROM lin_lb_infotesttype_price WHERE InfoTestTypeid=?","SELECT IID,InfoTestTypeid,CAST(price AS CHAR) AS price,IsActive,createtime,updatetime FROM lin_lb_infotesttype_price WHERE InfoTestTypeid=? ORDER BY IID",page,size,id);}
 @Transactional(isolation=Isolation.READ_COMMITTED) public long create(ItemCommand command){return save(null,command);}
 @Transactional(isolation=Isolation.READ_COMMITTED) public long update(long id,ItemCommand command){return save(id,command);}
 private long save(Long id,ItemCommand c){
  writable();if(c==null||c.name()==null||c.name().isBlank()||c.code()==null||c.code().isBlank()||c.name().codePointCount(0,c.name().length())>250||c.code().codePointCount(0,c.code().length())>250)throw error(400,"检测项目名和检测编号不能为空且最多250字");
  if(c.type()!=null&&(c.type()<0||c.type()>4))throw error(400,"所属大类无效");active(c.active());key(c.requestKey());
  if(id!=null){id(id);if(c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}"))throw error(400,"请重新打开项目后编辑");}
  int actor=actor();var payload=new TreeMap<String,Object>();payload.put("operation",id==null?"CREATE_ITEM":"UPDATE_ITEM");payload.put("id",id);payload.put("name",c.name());payload.put("code",c.code());payload.put("type",id==null&&c.type()==null?Integer.valueOf(0):c.type());payload.put("active",id==null&&c.active()==null?Integer.valueOf(1):c.active());payload.put("expectedToken",id==null?null:c.expectedToken());
  String canonical=store.encode(payload);Long previous=replay(actor,c.requestKey(),canonical);if(previous!=null)return previous;
  Map<String,Object> before=null;long result;
  if(id==null){result=store.insert("INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive) VALUES(?,?,0,0,0,0,0,'',?,?,?)",c.name(),c.code(),LocalDateTime.now(ZoneId.of("Asia/Shanghai")),c.type()==null?0:c.type(),c.active()==null?1:c.active());}
  else{before=raw(id,true);if(!hash(store.encode(before)).equals(c.expectedToken()))throw error(409,"项目资料已变更，请刷新核对后保存");store.jdbc().update("UPDATE lin_lb_infotesttype SET TestName=?,TestNO=?,type=?,IsActive=? WHERE InfoTestTypeId=?",c.name(),c.code(),c.type()==null?before.get("type"):c.type(),c.active()==null?before.get("IsActive"):c.active(),id);result=id;}
  audit(actor,c.requestKey(),canonical,"ITEM",result,before,raw(result,false));invalidate();return result;
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public long addPrice(long testId,PriceCommand c){
  writable();id(testId);if(c==null||c.price()==null||c.price().stripTrailingZeros().scale()>2||c.price().abs().compareTo(new BigDecimal("9999999999999999.99"))>0)throw error(400,"价格须在原金额范围内且最多两位小数");active(c.active());key(c.requestKey());int actor=actor();
  var payload=new TreeMap<String,Object>();payload.put("operation","ADD_PRICE");payload.put("testId",testId);payload.put("price",c.price().setScale(2).toPlainString());payload.put("active",c.active()==null?1:c.active());String canonical=store.encode(payload);Long previous=replay(actor,c.requestKey(),canonical);if(previous!=null)return previous;
  raw(testId,true);long id=store.insert("INSERT INTO lin_lb_infotesttype_price(InfoTestTypeid,price,IsActive) VALUES(?,?,?)",testId,c.price(),c.active()==null?1:c.active());
  audit(actor,c.requestKey(),canonical,"PRICE",id,null,store.one("SELECT IID,InfoTestTypeid,price,IsActive,createtime,updatetime FROM lin_lb_infotesttype_price WHERE IID=?",id));invalidate();return id;
 }
 private Map<String,Object> priceRaw(long testId,long priceId,boolean lock){
  if(priceId<1)throw error(400,"价格记录编号无效");
  return store.one("SELECT IID,InfoTestTypeid,CAST(price AS CHAR) AS price,IsActive,createtime,updatetime FROM lin_lb_infotesttype_price WHERE IID=? AND InfoTestTypeid=?"+(lock?" FOR UPDATE":""),priceId,testId);
 }
 @Transactional(readOnly=true) public Map<String,Object> price(long testId,long priceId){read();raw(testId,false);var row=new LinkedHashMap<>(priceRaw(testId,priceId,false));row.put("editToken",hash(store.encode(row)));return row;}
 @Transactional(isolation=Isolation.READ_COMMITTED) public long updatePrice(long testId,long priceId,PriceEditCommand c){
  if(c==null)throw error(400,"价格维护内容缺失");return changePrice(testId,priceId,c.price(),c.active(),c.requestKey(),c.expectedToken(),false);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) public long deletePrice(long testId,long priceId,PriceDeleteCommand c){
  if(c==null)throw error(400,"价格维护内容缺失");return changePrice(testId,priceId,null,null,c.requestKey(),c.expectedToken(),true);
 }
 private long changePrice(long testId,long priceId,BigDecimal value,Integer state,String request,String token,boolean deleting){
  var p=policy(true);if(!p.writable()||deleting&&!p.deletable())throw error(409,deleting?"价格删除尚未开启":"原检测项目维护尚未开启");
  id(testId);if(priceId<1)throw error(400,"价格记录编号无效");key(request);if(token==null||!token.matches("[a-f0-9]{64}"))throw error(400,"请重新打开价格后操作");
  if(!deleting&&(value==null||value.stripTrailingZeros().scale()>4||value.abs().compareTo(new BigDecimal("9999999999999999.99"))>0))throw error(400,"编辑价格须在原金额范围内且最多四位小数");active(state);
  int actor=actor();var payload=new TreeMap<String,Object>();payload.put("operation",deleting?"DELETE_PRICE":"UPDATE_PRICE");payload.put("testId",testId);payload.put("priceId",priceId);payload.put("price",value==null?null:value.stripTrailingZeros().toPlainString());payload.put("active",state);payload.put("expectedToken",token);String canonical=store.encode(payload);Long previous=replay(actor,request,canonical);if(previous!=null)return previous;
  raw(testId,true);var before=priceRaw(testId,priceId,true);if(!hash(store.encode(before)).equals(token))throw error(409,"价格记录已变更，请刷新核对后操作");
  Object after;
  if(deleting){store.jdbc().update("DELETE FROM lin_lb_infotesttype_price WHERE IID=? AND InfoTestTypeid=?",priceId,testId);after=Map.of("deleted",true,"priceId",priceId,"testId",testId);}
  else{store.jdbc().update("UPDATE lin_lb_infotesttype_price SET price=?,IsActive=? WHERE IID=? AND InfoTestTypeid=?",value,state==null?before.get("IsActive"):state,priceId,testId);after=priceRaw(testId,priceId,false);}
  audit(actor,request,canonical,"PRICE",priceId,before,after);invalidate();return priceId;
 }
 private void writable(){if(!policy(true).writable())throw error(409,"原检测项目维护尚未开启");}
 private int actor(){int id=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",id);return id;}
 private Long replay(int actor,String key,String canonical){var rows=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_TEST_CATALOG' AND request_id=? ORDER BY id DESC LIMIT 1",actor,key);if(rows.isEmpty())return null;try{var json=new ObjectMapper().readTree(String.valueOf(rows.get(0).get("after_json")));if(!canonical.equals(json.path("canonicalRequest").asText())||!json.path("resultId").canConvertToLong())throw error(409,"请求号已用于其他项目操作");return json.path("resultId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw error(409,"原项目请求无法校验");}}
 private void audit(int actor,String request,String canonical,String kind,long id,Object before,Object after){store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_TEST_CATALOG_SAVED','LEGACY_TEST_CATALOG',?,?,?,?)",actor,kind+":"+id,request,before==null?null:store.encode(before),store.encode(Map.of("canonicalRequest",canonical,"resultId",id,"record",after)));}
 private void invalidate(){if(cacheManager==null)return;Runnable clear=()->{var cache=cacheManager.getCache("dictionary");if(cache!=null)cache.clear();};if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){clear.run();}});else clear.run();}
 private static void id(long id){if(id<1||id>4294967295L)throw error(400,"检测项目编号无效");}
 private static void paging(int page,int size){if(page<0||size<1||size>100)throw error(400,"分页无效");}
 private static void key(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{16,64}"))throw error(400,"项目请求号无效");}
 private static void active(Integer value){if(value!=null&&value!=0&&value!=1)throw error(400,"状态无效");}
 private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static BusinessException error(int code,String text){return new BusinessException(code,text);}
}
