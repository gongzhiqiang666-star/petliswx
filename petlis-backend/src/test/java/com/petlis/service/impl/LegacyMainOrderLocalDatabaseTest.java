package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyMainOrderLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyMainOrderService service;AccessService access;int actor;long order;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));String prefix="it_mainorder_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'主单测试','normal')",prefix);order=LegacyDetailFixtures.order(store,prefix,actor);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET CollectDt='2024-05-06',TestFee=125.50,IsPayMoney=1 WHERE ListDiagnosisProjectId=?",order);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyMainOrderService(store,access);gate(true,true);}
 void gate(boolean read,boolean write){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-main-orders',?,'transaction-only main order test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",read,"writeEnabled",write)),actor);}
 String key(){return "main_"+UUID.randomUUID().toString().replace("-","");}
 void mapListAdmin(String groups,String status){
  long admin=store.insert("INSERT INTO lin_admin(username,datagroup,status) VALUES(?,?,?)","it_scope_"+actor,groups,status);
  store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0",store.encode(Map.of("enabled",true,"writeEnabled",true,"adminMappings",Map.of(String.valueOf(actor),admin))));
 }
 @Test void listUsesMappedOriginalGroupsAndExcludesNegativeStates(){
  long group=900000000L+actor;mapListAdmin(String.valueOf(group),"normal");
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET CreateDataTypeIId=?,SampleStatus=2 WHERE ListDiagnosisProjectId=?",group,order);
  long foreign=LegacyDetailFixtures.order(store,"it_scope_foreign_"+actor,actor),hidden=LegacyDetailFixtures.order(store,"it_scope_hidden_"+actor,actor);
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET CreateDataTypeIId=?,SampleStatus=2 WHERE ListDiagnosisProjectId=?",group+1,foreign);
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET CreateDataTypeIId=?,SampleStatus=-1 WHERE ListDiagnosisProjectId=?",group,hidden);
  var result=service.list(null,0,100);assertEquals(1,((Number)result.get("total")).intValue());
  @SuppressWarnings("unchecked") var items=(List<Map<String,Object>>)result.get("items");assertEquals(order,((Number)items.get(0).get("orderId")).longValue());
  assertEquals(0,((Number)service.list(-1,0,100).get("total")).intValue());
  assertEquals(1,((Number)service.list(2,0,100).get("total")).intValue());
  store.jdbc().update("UPDATE lin_admin SET datagroup=? WHERE username=?",group+","+(group+1)+","+group,"it_scope_"+actor);
  assertEquals(2,((Number)service.list(null,0,100).get("total")).intValue());
 }
 @Test void listDoesNotTreatJavaAdminAsAllOriginalGroups(){
  assertEquals(409,assertThrows(BusinessException.class,()->service.list(null,0,20)).getCode());
  mapListAdmin("1,2","hidden");assertEquals(403,assertThrows(BusinessException.class,()->service.list(null,0,20)).getCode());
 }
 @Test void emptyOriginalGroupHasNoRecordsAndMalformedGroupFailsClosed(){
  mapListAdmin("","normal");assertEquals(0,((Number)service.list(null,0,20).get("total")).intValue());
  store.jdbc().update("UPDATE lin_admin SET datagroup=? WHERE username=?","1,2 OR 1=1","it_scope_"+actor);
  assertEquals(409,assertThrows(BusinessException.class,()->service.list(null,0,20)).getCode());
 }
 LegacyMainOrderService.Command command(Map<String,String> values){return new LegacyMainOrderService.Command(values,key(),service.detail(order).get("editToken").toString());}
 @Test void hospitalSettlementRetainsOriginalTypeAndGeneratesOnlyConfiguredLink(){
  long hospital=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES('test settlement','test',1,0)");
  assertEquals("月结",service.hospitalSettlement(hospital).get("message"));assertEquals(false,service.hospitalSettlement(hospital).get("linkConfigured"));
  service=new LegacyMainOrderService(store,access,null,new LegacyReportLinks("https://petlis.invalid/","petlis-public-test-key"));var result=service.hospitalSettlement(hospital);assertEquals(true,result.get("linkConfigured"));assertTrue(result.get("url").toString().contains("?HospID="));
  store.jdbc().update("UPDATE lin_lb_infohospital SET PayType=0 WHERE InfoHospitalId=?",hospital);assertEquals("次结",service.hospitalSettlement(hospital).get("message"));assertEquals(false,service.hospitalSettlement(0).get("found"));
 }
 @Test void groupNumberUsesLatestOrderAndIgnoresDisplayedStartNumberForCalculation(){
  long group=store.insert("INSERT INTO lin_lb_kuhuirightdatagroup(GroupName,GroupRemark,StartWord,StartNO,ParentIID,CreateDt,UpdateDt) VALUES('test group','','X-','90000','0',NOW(),NOW())");
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET CreateDataTypeIId=?,UserNO='X-00009' WHERE ListDiagnosisProjectId=?",group,order);
  var result=service.groupNumber(group);assertEquals("X-00010",result.get("userno"));assertEquals("90000",result.get("startNo"));assertEquals(false,result.get("reserved"));assertEquals(result,service.groupNumber(group));assertEquals("X-00009",service.detail(order).get("UserNO"));
  long empty=store.insert("INSERT INTO lin_lb_kuhuirightdatagroup(GroupName,GroupRemark,StartWord,StartNO,ParentIID,CreateDt,UpdateDt) VALUES('empty group','','','99999','0',NOW(),NOW())");assertEquals("00001",service.groupNumber(empty).get("userno"));
 }
 @Test void choicesRetainDisabledIdsAndRejectTableInjection(){
  String name="it_main_choice_"+UUID.randomUUID().toString().substring(0,8);
  long id=store.insert("INSERT INTO lin_lb_infoanimal(AnimalName,AnimalName_en,IsActive,createtime,updatetime) VALUES(?,?,0,UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",name,name);
  var result=service.choices("species",name,null,0,20);assertEquals(1,((Number)result.get("total")).intValue());
  @SuppressWarnings("unchecked") var rows=(List<Map<String,Object>>)service.choices("species","not-matching",id,0,1).get("items");assertEquals(String.valueOf(id),rows.get(0).get("id"));assertEquals(name,rows.get(0).get("name"));
  long variety=store.insert("INSERT INTO lin_lb_infovariety(Variety_CN,Variety_EN,createtime,updatetime) VALUES(?,?,UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",name,"Breed reference");
  @SuppressWarnings("unchecked") var varieties=(List<Map<String,Object>>)service.choices("variety",null,variety,0,1).get("items");assertEquals(name,varieties.get(0).get("Variety_CN"));assertEquals("Breed reference",varieties.get(0).get("Variety_EN"));
  assertEquals(400,assertThrows(BusinessException.class,()->service.choices("lin_user","",null,0,20)).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.choices("species","",null,-1,20)).getCode());
 }
 @Test void autoCreateAddsExactlyOneOriginalFreightDetailAndBodyCopy(){
  var admin=store.one("SELECT id,datagroup FROM lin_admin WHERE status='normal' AND datagroup<>'' ORDER BY id LIMIT 1");
  store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0",store.encode(Map.of("enabled",true,"writeEnabled",true,"createEnabled",true,"adminMappings",Map.of(String.valueOf(actor),admin.get("id")))));
  store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-order-details',?,'transaction-only auto test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",true,"writeEnabled",true)),actor);
  var detailService=new LegacyOrderDetailService(store,access,new LegacyReportLinks("https://petlis.invalid/","petlis-public-test-key"));service=new LegacyMainOrderService(store,access,detailService);
  var c=new LegacyMainOrderService.CreateCommand(true,key());long id=service.create(c);assertEquals(id,service.create(c));
  var row=store.one("SELECT DiagnosisDetailId,InfoTestTypeId,TestPrice,IsActive FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",id);assertEquals(246,((Number)row.get("InfoTestTypeId")).intValue());assertEquals(1,((Number)row.get("IsActive")).intValue());
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",Integer.class,id));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id=?",Integer.class,row.get("DiagnosisDetailId")));
  Object originalPrice=store.one("SELECT price FROM lin_lb_infotesttype_price WHERE InfoTestTypeid=246 LIMIT 1").get("price");assertEquals(0,new java.math.BigDecimal(originalPrice.toString()).compareTo(new java.math.BigDecimal(row.get("TestPrice").toString())));
 }
 @Test void adminCreateUsesMappedOriginalDataGroupAndExactDefaults(){
  var c=new LegacyMainOrderService.CreateCommand(false,key());assertEquals(409,assertThrows(BusinessException.class,()->service.create(c)).getCode());
  var admin=store.one("SELECT id,datagroup FROM lin_admin WHERE status='normal' AND datagroup<>'' ORDER BY id LIMIT 1");
  store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0",store.encode(Map.of("enabled",true,"writeEnabled",true,"createEnabled",true,"adminMappings",Map.of(String.valueOf(actor),admin.get("id")))));
  long id=service.create(c);assertEquals(id,service.create(c));var row=service.detail(id);assertEquals("55550-",row.get("UserNO"));
  @SuppressWarnings("unchecked") var listed=(List<Map<String,Object>>)service.list(null,0,100).get("items");
  assertTrue(listed.stream().anyMatch(item->((Number)item.get("orderId")).longValue()==id&&"未指定医院".equals(item.get("hospitalName"))));assertEquals(0,((Number)row.get("InfoHospitalId")).intValue());assertEquals(4,((Number)row.get("InfoAnimalId")).intValue());assertEquals(1,((Number)row.get("DictSexId")).intValue());assertEquals(1,((Number)row.get("InfoSampleTypeId")).intValue());assertEquals(2,((Number)row.get("SampleStatus")).intValue());assertEquals("",row.get("AnimalName"));assertEquals(0,((Number)row.get("IsPayMoney")).intValue());assertEquals(0,((Number)row.get("CreateSampleId")).intValue());
  var groups=Arrays.asList(admin.get("datagroup").toString().split(","));long expected=groups.stream().map(Long::parseLong).anyMatch(v->v==2)?2:Long.parseLong(groups.get(0));assertEquals(expected,((Number)row.get("CreateDataTypeIId")).longValue());
 }
 @Test void editPreservesEmptyDatesFinancialFieldsAndReplays(){var before=service.detail(order);var c=command(Map.of("AnimalName","修改宠物","MasterName","修改主人","CollectDt","","Birthday","0"));assertEquals(order,service.update(order,c));assertEquals(order,service.update(order,c));var row=service.detail(order);assertEquals("修改宠物",row.get("AnimalName"));assertEquals(before.get("CollectDt"),row.get("CollectDt"));assertEquals(before.get("Birthday"),row.get("Birthday"));assertEquals(before.get("TestFee"),row.get("TestFee"));assertEquals(before.get("IsPayMoney"),row.get("IsPayMoney"));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_MAIN_ORDER'",Integer.class,actor));}
 @Test void staleSnapshotsAndUnlistedFieldsAreRejected(){var c=command(Map.of("AnimalName","new"));service.update(order,c);assertEquals(409,assertThrows(BusinessException.class,()->service.update(order,new LegacyMainOrderService.Command(Map.of("MasterName","other"),key(),c.expectedToken()))).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.update(order,command(Map.of("TestFee","0")))).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.update(order,command(Map.of("AnimalName;DROP TABLE lin_user","x")))).getCode());}
 @Test void closedFlagsAndNonAdminAreRejected(){gate(true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.update(order,new LegacyMainOrderService.Command(Map.of("AnimalName","x"),key(),"a".repeat(64)))).getCode());gate(false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.detail(order)).getCode());doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.capabilities()).getCode());}
 @Test void richRemarkRoundTripsWithoutChangingOtherFieldsAndReorderedRetryIsSameRequest(){
  String html="<p style=\"text-align:center\"><strong>送检说明</strong></p><table border=\"1\"><tbody><tr><td colspan=\"2\">样本已冷藏</td></tr></tbody></table><p><img src=\"/owned-example.png\" width=\"12\" alt=\"示意图\"></p>";
  var before=new LinkedHashMap<>(store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order));var fields=new LinkedHashMap<String,String>();fields.put("Remark",html);fields.put("DoctorName","合成医师");var c=command(fields);service.update(order,c);
  var reversed=new LinkedHashMap<String,String>();reversed.put("DoctorName","合成医师");reversed.put("Remark",html);assertEquals(order,service.update(order,new LegacyMainOrderService.Command(reversed,c.requestKey(),c.expectedToken())));
  before.put("Remark",html);before.put("DoctorName","合成医师");assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order));assertEquals(html,service.detail(order).get("Remark"));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_MAIN_ORDER' AND resource_id=? AND request_id=?",Integer.class,String.valueOf(order),c.requestKey()));
 }

}
