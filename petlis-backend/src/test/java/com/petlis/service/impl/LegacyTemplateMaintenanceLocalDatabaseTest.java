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
class LegacyTemplateMaintenanceLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyTemplateMaintenanceService service;AccessService access;int actor;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_tpl_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'模板维护测试','normal')",prefix);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyTemplateMaintenanceService(store,access);gate(true,true,true);}
 void gate(boolean enabled,boolean write,boolean delete){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-templates',?,'transaction-only template test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",enabled,"writeEnabled",write,"deleteEnabled",delete)),actor);}
 String key(){return "template_"+UUID.randomUUID().toString().replace("-","");}
 LegacyTemplateMaintenanceService.Command command(String title,String html,long project,String token){return new LegacyTemplateMaintenanceService.Command(title,html,project,key(),token);}
 @Test void reportCreateUpdatePreservesBodyProjectAndOriginalTimestampSemantics(){
  String html="<h2>检测报告 {{标本号}}</h2><table><tr><td>α / 1.20</td></tr></table><img src=\"https://petlis.invalid/test.png\">";
  var c=command(prefix,html,0,null);long id=service.save("report",null,c);assertEquals(id,service.save("report",null,c));
  var before=service.detail("report",id);assertEquals(html,before.get("html"));assertEquals(0,((Number)before.get("projectId")).intValue());
  var raw=store.one("SELECT CreateDt,updatetime FROM lin_lb_infotesttypemodel WHERE InfoTestTypeModelId=?",id);assertNotNull(raw.get("CreateDt"));assertNull(raw.get("updatetime"));
  service.save("report",id,command(prefix+"改",html+"<p>English</p>",246,before.get("editToken").toString()));
  var after=service.detail("report",id);assertEquals(246,((Number)after.get("projectId")).intValue());assertEquals(html+"<p>English</p>",after.get("html"));assertEquals(raw,store.one("SELECT CreateDt,updatetime FROM lin_lb_infotesttypemodel WHERE InfoTestTypeModelId=?",id));
  assertEquals(1,((Number)service.list("report",246L,prefix,0,20).get("total")).intValue());assertEquals(0,((Number)service.list("report",0L,prefix,0,20).get("total")).intValue());
  store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-order-details','{\"enabled\":true}','transaction-only template reader',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",actor);
  var reader=new LegacyOrderDetailService(store,access,new LegacyReportLinks("https://petlis.invalid/","public-template-test-key"));assertEquals(after.get("html"),reader.reportTemplate(id).get("displayHtml"));
  @SuppressWarnings("unchecked") var options=(List<Map<String,Object>>)reader.testTemplates(246,0,100).get("items");assertTrue(options.stream().anyMatch(item->((Number)item.get("InfoTestTypeModelId")).longValue()==id));
 }
 @Test void mailAllowsEmptyBodyAndUpdatesSecondsWithoutChangingCreationTime(){
  long id=service.save("mail",null,command(prefix,"",0,null));var row=service.detail("mail",id);
  assertEquals("",row.get("html"));store.jdbc().update("UPDATE lin_lb_dictmaildiagnosisreport SET createtime=123,updatetime=124 WHERE id=?",id);
  row=service.detail("mail",id);service.save("mail",id,command(prefix,"<p>邮件正文</p>",0,row.get("editToken").toString()));
  var raw=store.one("SELECT createtime,updatetime FROM lin_lb_dictmaildiagnosisreport WHERE id=?",id);assertEquals(123,((Number)raw.get("createtime")).intValue());assertTrue(((Number)raw.get("updatetime")).longValue()>124);
 }
 @Test void staleVersionAndRequestReuseWithDifferentContentAreRejected(){
  var c=command(prefix,"<p>original</p>",0,null);long id=service.save("report",null,c);var before=service.detail("report",id);
  var edit=command(prefix,"<p>changed</p>",0,before.get("editToken").toString());assertEquals(id,service.save("report",id,edit));assertEquals(id,service.save("report",id,edit));
  assertEquals(409,assertThrows(BusinessException.class,()->service.save("report",id,command(prefix,"<p>stale</p>",0,before.get("editToken").toString()))).getCode());
  assertEquals(409,assertThrows(BusinessException.class,()->service.save("report",null,new LegacyTemplateMaintenanceService.Command(prefix,"different",0,c.requestKey(),null))).getCode());
 }
 @Test void deleteIsGuardedAuditedAndReplaySafe(){
  long id=service.save("report",null,command(prefix,"<p>template</p>",0,null));var delete=new LegacyTemplateMaintenanceService.DeleteCommand(key(),service.detail("report",id).get("editToken").toString());
  gate(true,true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.delete("report",id,delete)).getCode());gate(true,true,true);
  assertEquals(id,service.delete("report",id,delete));assertEquals(id,service.delete("report",id,delete));assertEquals(404,assertThrows(BusinessException.class,()->service.detail("report",id)).getCode());
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND action='LEGACY_TEMPLATE_DELETED'",Integer.class,actor));
 }
 @Test void flagsUnknownKindsAndRequiredReportBodyAreEnforced(){
  assertEquals(400,assertThrows(BusinessException.class,()->service.save("report",null,command(prefix,"",0,null))).getCode());
  assertEquals(400,assertThrows(BusinessException.class,()->service.list("lin_user",null,"",0,20)).getCode());
  gate(true,false,false);assertEquals(409,assertThrows(BusinessException.class,()->service.save("mail",null,command(prefix,"",0,null))).getCode());gate(false,true,true);
  assertEquals(409,assertThrows(BusinessException.class,()->service.list("report",null,"",0,20)).getCode());
  doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.capabilities()).getCode());
 }
}
