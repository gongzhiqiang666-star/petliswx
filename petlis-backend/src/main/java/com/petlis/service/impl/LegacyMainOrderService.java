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
@Service @RequiredArgsConstructor(onConstructor_=@org.springframework.beans.factory.annotation.Autowired)
public class LegacyMainOrderService {
 private final FeatureStore store;private final AccessService access;private final LegacyOrderDetailService details;private final LegacyReportLinks links;
 public LegacyMainOrderService(FeatureStore store,AccessService access,LegacyOrderDetailService details){this(store,access,details,new LegacyReportLinks("",""));}
 public LegacyMainOrderService(FeatureStore store,AccessService access){this(store,access,null);}
 public record CreateCommand(boolean autoLogistics,String requestKey){}
 public record Command(Map<String,String> fields,String requestKey,String expectedToken){}
 private static final Set<String> ALLOWED=Set.of("Age","AnimalName","AnimalName_en","AnimalType","AnimalTypeId","AnimalType_en","Birthday","ChipNO","CityId","CollectDt","CreateDataTypeIId","DiagnosticResults","DictSexId","DoctorName","InfoAnimalId","InfoHospitalId","InfoSampleTypeId","MarketingName","MasterName","MasterName_en","ReceiveSampleDt","Remark","ReportNO","SampleArea","SampleStatus","SendReportDt","SendSampleDt","UserNO");
 private static final Set<String> INTEGERS=Set.of("AnimalTypeId","CityId","CreateDataTypeIId","DictSexId","InfoAnimalId","InfoHospitalId","InfoSampleTypeId","SampleStatus");
 private static final Set<String> DATES=Set.of("Birthday","CollectDt","ReceiveSampleDt","SendReportDt","SendSampleDt");
 private boolean[] policy(boolean lock){access.requireAdmin();var rows=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'"+(lock?" FOR UPDATE":""));if(rows.size()!=1)return new boolean[]{false,false,false};try{var j=new ObjectMapper().readTree(rows.get(0).get("config_value").toString());boolean read=j.path("enabled").isBoolean()&&j.path("enabled").asBoolean();boolean write=read&&j.path("writeEnabled").isBoolean()&&j.path("writeEnabled").asBoolean();return new boolean[]{read,write,write&&j.path("createEnabled").isBoolean()&&j.path("createEnabled").asBoolean()};}catch(Exception e){throw new BusinessException(409,"原主单参数无效");}}
 @Transactional(readOnly=true) public Map<String,Boolean> capabilities(){var p=policy(false);return Map.of("mainOrders",p[0],"mainOrderWrites",p[1],"mainOrderCreates",p[2]);}
 private Map<String,Object> raw(long id,boolean lock){if(id<1||id>4294967295L)throw new BusinessException(400,"主单编号无效");return new LinkedHashMap<>(store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?"+(lock?" FOR UPDATE":""),id));}
 private String token(Map<String,Object> row){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.encode(new TreeMap<>(row)).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private static final Map<String,List<String>> CHOICES=Map.of(
  "dataGroup",List.of("lin_lb_kuhuirightdatagroup","IID","GroupName"),
  "hospital",List.of("lin_lb_infohospital","InfoHospitalId","HospitalName"),
  "species",List.of("lin_lb_infoanimal","InfoAnimalId","AnimalName"),
  "sex",List.of("lin_lb_dictsex","DictSexId","SexName"),
  "sampleType",List.of("lin_lb_infosampletype","InfoSampleTypeId","SampleName"),
  "variety",List.of("lin_lb_infovariety","IID","Variety_CN"));
 @Transactional(readOnly=true) public Map<String,Object> choices(String kind,String q,Long selectedId,int page,int size){
  if(!policy(false)[0])throw new BusinessException(409,"原主单尚未开启");var d=CHOICES.get(kind);if(d==null)throw new BusinessException(400,"未知选项类型");
  if(page<0||size<1||size>100||q!=null&&q.length()>250||selectedId!=null&&selectedId<1)throw new BusinessException(400,"选项查询无效");
  String table=d.get(0),id=d.get(1),name=d.get(2),where=selectedId==null?" WHERE "+name+" LIKE ?":" WHERE "+id+"=?";Object value=selectedId==null?"%"+(q==null?"":q)+"%":selectedId;
  // Original form has no IsActive filter; historical disabled choices must remain reachable.
  var result=store.page("SELECT COUNT(*) FROM "+table+where,"SELECT CAST("+id+" AS CHAR) AS id,"+name+" AS name"+(kind.equals("variety")?",Variety_CN,Variety_EN":"")+" FROM "+table+where+" ORDER BY "+id,page,size,value);
  @SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)result.get("items");for(var row:items)row.put("name",LegacyDictionarySelector.display(row.get("name")));return result;
 }
 @Transactional(readOnly=true) public Map<String,Object> hospitalSettlement(long id){
  if(!policy(false)[0])throw new BusinessException(409,"原主单尚未开启");if(id<0||id>4294967295L)throw new BusinessException(400,"医院编号无效");
  var rows=store.jdbc().queryForList("SELECT InfoHospitalId,PayType FROM lin_lb_infohospital WHERE InfoHospitalId=?",id);if(rows.isEmpty())return Map.of("found",false,"message","未找到医院信息");
  var result=new LinkedHashMap<String,Object>();result.put("found",true);result.put("hospitalId",id);result.put("message",Objects.toString(rows.get(0).get("PayType"),"0").equals("1")?"月结":"次结");
  try{result.put("url",links.hospital(id));result.put("linkConfigured",true);}catch(BusinessException e){if(e.getCode()!=409)throw e;result.put("linkConfigured",false);result.put("linkMessage",e.getMessage());}return result;
 }
 @Transactional(readOnly=true) public Map<String,Object> groupNumber(long groupId){
  if(!policy(false)[0])throw new BusinessException(409,"原主单尚未开启");if(groupId<1)throw new BusinessException(400,"数据组编号无效");
  var group=store.one("SELECT IID,GroupName,StartWord,StartNO FROM lin_lb_kuhuirightdatagroup WHERE IID=?",groupId);
  var rows=store.jdbc().queryForList("SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE CreateDataTypeIId=? ORDER BY ListDiagnosisProjectId DESC LIMIT 1",groupId);
  String latest=rows.isEmpty()?null:Objects.toString(rows.get(0).get("UserNO"),"");
  long count=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_listdiagnosisproject WHERE CreateDataTypeIId=?",Long.class,groupId);
  String prefix=Objects.toString(group.get("StartWord"),"");
  return Map.of("userno",LegacyGroupNumberRules.preview(prefix,latest,count),"groupName",Objects.toString(group.get("GroupName"),""),"prefix",prefix,"startNo",Objects.toString(group.get("StartNO"),""),"latest",latest==null?"0":latest,"reserved",false);
 }
 @Transactional(readOnly=true) public Map<String,Object> list(Integer status,int page,int size){
  if(!policy(false)[0])throw new BusinessException(409,"原主单尚未开启");if(page<0||size<1||size>100)throw new BusinessException(400,"分页无效");
  String from=" FROM lin_lb_listdiagnosisproject o LEFT JOIN lin_lb_infohospital h ON h.InfoHospitalId=o.InfoHospitalId LEFT JOIN lin_order_extension e ON e.order_id=o.ListDiagnosisProjectId"+(status==null?"":" WHERE o.SampleStatus=?");
  String columns="SELECT o.ListDiagnosisProjectId AS orderId,o.UserNO AS orderNo,COALESCE(h.HospitalName,'未指定医院') AS hospitalName,o.AnimalName AS animalName,o.TestFee AS amount,o.SampleStatus AS status,o.IsPayMoney AS paid,o.CreateDt AS createdAt,e.settlement_mode AS settlementMode,EXISTS(SELECT 1 FROM lin_audit_record r WHERE r.resource_type='MOCK_REFUND' AND r.action='MOCK_REFUND_COMPLETED' AND CAST(r.resource_id AS UNSIGNED)=o.ListDiagnosisProjectId) AS refunded";
  return store.page("SELECT COUNT(*)"+from,columns+from+" ORDER BY o.ListDiagnosisProjectId DESC",page,size,status==null?new Object[]{}:new Object[]{status});
 }
 @Transactional(readOnly=true) public Map<String,Object> detail(long id){if(!policy(false)[0])throw new BusinessException(409,"原送检主单尚未开启");var row=raw(id,false);row.put("editToken",token(row));return row;}
 @Transactional public long update(long id,Command c){
  if(!policy(true)[1])throw new BusinessException(409,"原送检主单维护尚未开启");
  if(c==null||c.fields()==null||c.fields().isEmpty()||c.fields().size()>ALLOWED.size()||!ALLOWED.containsAll(c.fields().keySet()))throw new BusinessException(400,"主单字段无效或不允许维护");
  if(c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}")||c.expectedToken()==null||!c.expectedToken().matches("[a-f0-9]{64}"))throw new BusinessException(400,"请重新打开主单后修改");
  var values=LegacyMainOrderRules.update(new LinkedHashMap<>(c.fields()));
  for(var entry:values.entrySet()){
   String name=entry.getKey(),value=Objects.toString(entry.getValue(),"");
   if(INTEGERS.contains(name)){try{if(name.equals("CreateDataTypeIId"))Long.parseLong(value);else Integer.parseInt(value);}catch(NumberFormatException e){throw new BusinessException(400,"主单整数字段无效："+name);}}
   else if(DATES.contains(name)){try{if(value.length()==10)java.time.LocalDate.parse(value);else java.time.LocalDateTime.parse(value.replace(' ','T'));}catch(java.time.DateTimeException e){throw new BusinessException(400,"主单日期无效："+name);}}
   else{int max=Set.of("AnimalName","AnimalName_en","MasterName","MasterName_en").contains(name)?250:name.equals("DiagnosticResults")?255:Set.of("DoctorName","MarketingName").contains(name)?100:name.equals("Remark")?65535:50;if(value.codePointCount(0,value.length())>max||name.equals("Remark")&&value.getBytes(StandardCharsets.UTF_8).length>65535)throw new BusinessException(400,"主单字段超过原长度："+name);}
  }
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);String canonical=store.encode(Map.of("id",id,"command",c));
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_MAIN_ORDER' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty())try{var j=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(j.path("canonicalRequest").asText()))throw new BusinessException(409,"请求号已用于其他主单操作");return id;}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"主单请求无法核对");}
  var before=raw(id,true);if(!token(before).equals(c.expectedToken()))throw new BusinessException(409,"主单已变更，请重新打开核对");
  if(!values.isEmpty()){var args=new ArrayList<Object>();var sets=new ArrayList<String>();new TreeMap<>(values).forEach((key,value)->{sets.add("`"+key+"`=?");args.add(value);});args.add(id);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET "+String.join(",",sets)+" WHERE ListDiagnosisProjectId=?",args.toArray());}
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,'LEGACY_MAIN_ORDER_UPDATED','LEGACY_MAIN_ORDER',?,?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(before),store.encode(Map.of("orderId",id,"canonicalRequest",canonical)));return id;
 }
 @Transactional public long create(CreateCommand c){
  if(!policy(true)[1])throw new BusinessException(409,"原主单维护尚未开启");
  var configs=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");
  com.fasterxml.jackson.databind.JsonNode config;try{config=new ObjectMapper().readTree(configs.get(0).get("config_value").toString());}catch(Exception e){throw new BusinessException(409,"主单参数无效");}
  if(!config.path("createEnabled").isBoolean()||!config.path("createEnabled").asBoolean())throw new BusinessException(409,"原后台新增尚未开启");
  if(c==null||c.requestKey()==null||!c.requestKey().matches("[A-Za-z0-9_-]{16,64}"))throw new BusinessException(400,"新增请求号无效");
  if(c.autoLogistics())store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE' FOR UPDATE");
  int actor=access.current().userId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);String canonical=store.encode(Map.of("operation","CREATE","command",c));
  var prior=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_MAIN_ORDER' AND request_id=? ORDER BY id DESC LIMIT 1",actor,c.requestKey());
  if(!prior.isEmpty())try{var json=new ObjectMapper().readTree(prior.get(0).get("after_json").toString());if(!canonical.equals(json.path("canonicalRequest").asText()))throw new BusinessException(409,"请求号已用于其他操作");return json.path("orderId").asLong();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"新增请求无法核对");}
  var mapped=config.path("adminMappings").path(String.valueOf(actor));if(!mapped.isIntegralNumber()||!mapped.canConvertToInt()||mapped.asInt()<1)throw new BusinessException(409,"请配置对应的原管理员身份");
  var admin=store.one("SELECT id,datagroup FROM lin_admin WHERE id=? AND status='normal' FOR SHARE",mapped.asInt());
  var groups=new ArrayList<Long>();for(String part:Objects.toString(admin.get("datagroup"),"").split(",")){try{long group=Long.parseLong(part);if(group>=0)groups.add(group);}catch(NumberFormatException e){throw new BusinessException(409,"原管理员数据组无效");}}
  if(groups.isEmpty())throw new BusinessException(409,"原管理员没有可用数据组");long group=groups.contains(2L)?2L:groups.get(0);
  var row=LegacyMainOrderRules.adminInsert(Map.of("SampleStatus",2,"CreateDataTypeIId",group,"InfoAnimalId",4,"DictSexId",1,"InfoSampleTypeId",1,"InfoHospitalId",1));
  row.put("SaveMedium",0);row.put("CreateDt",java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).withNano(0));
  var names=new ArrayList<String>();var values=new ArrayList<Object>();row.forEach((name,value)->{names.add("`"+name+"`");values.add(value);});
  long id=store.insert("INSERT INTO lin_lb_listdiagnosisproject("+String.join(",",names)+") VALUES("+String.join(",",Collections.nCopies(values.size(),"?"))+")",values.toArray());
  if(c.autoLogistics()){
   if(details==null)throw new BusinessException(409,"自动物流明细服务未配置");
   details.create(id,new LegacyOrderDetailService.CreateCommand(246,null,null,null,null,null,null,null,null,null,null,null,null,"auto_"+token(Map.of("request",c.requestKey())).substring(0,40)));
  }
  store.insert("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,'LEGACY_MAIN_ORDER_CREATED','LEGACY_MAIN_ORDER',?,?,?)",actor,String.valueOf(id),c.requestKey(),store.encode(Map.of("orderId",id,"legacyAdminId",mapped.asInt(),"canonicalRequest",canonical)));return id;
 }

}
