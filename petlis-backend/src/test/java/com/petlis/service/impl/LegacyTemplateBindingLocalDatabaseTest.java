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
class LegacyTemplateBindingLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyTemplateBindingService service;AccessService access;int actor;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_binding_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'变量绑定测试','normal')",prefix);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyTemplateBindingService(store,access);gate(true,true,true);}
 void gate(boolean enabled,boolean write,boolean delete){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-template-bindings',?,'transaction-only binding test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",enabled,"writeEnabled",write,"deleteEnabled",delete)),actor);}
 String key(){return "binding_"+UUID.randomUUID().toString().replace("-","");}
 @Test void maintenanceFeedsActualReportReplacementWithoutChangingSourceRecord(){
  var create=new LegacyTemplateBindingService.Command("ListDiagnosisProject","AnimalName",prefix,key(),null);long id=service.save(null,create);assertEquals(id,service.save(null,create));
  long order=LegacyDetailFixtures.order(store,prefix,actor);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET AnimalName='动物甲',MasterName='主人乙' WHERE ListDiagnosisProjectId=?",order);
  store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-order-details','{\"enabled\":true}','transaction-only binding reader',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",actor);
  var reader=new LegacyOrderDetailService(store,access,new LegacyReportLinks("https://petlis.invalid/","public-binding-test-key"));String body="<p>{"+prefix+"}</p>";
  assertEquals("<p>动物甲</p>",reader.renderBody(order,0,body,2,"https://petlis.invalid/report"));var row=service.detail(id);
  service.save(id,new LegacyTemplateBindingService.Command("ListDiagnosisProject","MasterName",prefix,key(),row.get("editToken").toString()));assertEquals("<p>主人乙</p>",reader.renderBody(order,0,body,2,"https://petlis.invalid/report"));
  assertEquals("动物甲",store.one("SELECT AnimalName FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order).get("AnimalName"));
  var delete=new LegacyTemplateBindingService.DeleteCommand(key(),service.detail(id).get("editToken").toString());assertEquals(id,service.delete(id,delete));assertEquals(id,service.delete(id,delete));assertEquals(body,reader.renderBody(order,0,body,2,"https://petlis.invalid/report"));
 }
 @Test void staleAndConflictingReplayDoNotOverwriteBinding(){
  String key=key();long id=service.save(null,new LegacyTemplateBindingService.Command("ListDiagnosisProject","UserNO",prefix,key,null));var old=service.detail(id);
  var edit=new LegacyTemplateBindingService.Command("ListDiagnosisProject","AnimalName",prefix,key(),old.get("editToken").toString());assertEquals(id,service.save(id,edit));assertEquals(id,service.save(id,edit));
  assertEquals(409,assertThrows(BusinessException.class,()->service.save(id,new LegacyTemplateBindingService.Command("InfoHospital","HospitalName",prefix,key(),old.get("editToken").toString()))).getCode());
  assertEquals(409,assertThrows(BusinessException.class,()->service.save(null,new LegacyTemplateBindingService.Command("InfoHospital","HospitalName",prefix,key,null))).getCode());
 }
 @Test void freeTextAndDuplicatesRemainDataNotExecutableIdentifiers(){
  var command=new LegacyTemplateBindingService.Command("custom table; no SQL","custom column",prefix,key(),null);long id=service.save(null,command);long second=service.save(null,new LegacyTemplateBindingService.Command(command.bindTable(),command.columnName(),prefix,key(),null));assertNotEquals(id,second);
  assertEquals("custom table; no SQL",service.detail(id).get("BindTable"));assertEquals(2,((Number)service.list(prefix,0,20).get("total")).intValue());
 }
 @Test void gatesLengthsAndAdminAreEnforced(){
  assertEquals(400,assertThrows(BusinessException.class,()->service.save(null,new LegacyTemplateBindingService.Command("","column",prefix,key(),null))).getCode());
  assertEquals(400,assertThrows(BusinessException.class,()->service.save(null,new LegacyTemplateBindingService.Command("a".repeat(51),"column",prefix,key(),null))).getCode());
  long id=service.save(null,new LegacyTemplateBindingService.Command("ListDiagnosisProject","UserNO",prefix,key(),null));var delete=new LegacyTemplateBindingService.DeleteCommand(key(),service.detail(id).get("editToken").toString());
  gate(true,true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.delete(id,delete)).getCode());gate(true,false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.save(null,new LegacyTemplateBindingService.Command("a","b",prefix,key(),null))).getCode());
  gate(false,true,true);assertEquals(409,assertThrows(BusinessException.class,()->service.list("",0,20)).getCode());doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.capabilities()).getCode());
 }
}
