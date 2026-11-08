package com.petlis.service.impl;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@SpringBootTest @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyOrderDetailAtomicLocalDatabaseTest {
 @SpyBean FeatureStore store;@MockBean AccessService access;@Autowired LegacyOrderDetailService service;int actor,test;long order;String prefix;org.springframework.jdbc.core.JdbcTemplate jdbc;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_detail_atomic_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'明细原子测试','normal')",prefix);order=LegacyDetailFixtures.order(store,prefix,actor);test=LegacyDetailFixtures.test(store,prefix);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));jdbc=spy(store.jdbc());doReturn(List.of(Map.of("config_value","{\"enabled\":true,\"writeEnabled\":true}"))).when(jdbc).queryForList(contains("FROM lin_operational_config"));doReturn(jdbc).when(store).jdbc();}
 @AfterEach void cleanup(){reset(store);if(order>0){assertEquals(prefix,store.one("SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order).get("UserNO"));store.jdbc().update("DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id IN (SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?)",order);store.jdbc().update("DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",order);store.jdbc().update("DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? AND UserNO=?",order,prefix);}store.jdbc().update("DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId=? AND TestNO=?",test,prefix);store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_ORDER_DETAIL'",actor);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);}
 LegacyOrderDetailService.CreateCommand command(){return new LegacyOrderDetailService.CreateCommand(test,BigDecimal.ONE,"10",BigDecimal.ONE,"",0,"",0,0,0,0,prefix,prefix,"detail_"+UUID.randomUUID().toString().replace("-",""));}
 void assertEmpty(){assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",Integer.class,order));assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail_info WHERE MailReport=?",Integer.class,prefix));}
 @Test void bodyCopyFailureRollsBackDetail(){doThrow(new IllegalStateException("synthetic body failure")).when(jdbc).update(startsWith("INSERT INTO lin_lb_diagnosisdetail_info"),any(Object[].class));assertThrows(IllegalStateException.class,()->service.create(order,command()));assertEmpty();}
 @Test void auditFailureRollsBackDetailAndBody(){doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));assertThrows(IllegalStateException.class,()->service.create(order,command()));assertEmpty();}
 @Test void updateAuditFailureRollsBackParentBodyAndPdfPath(){
  doReturn(List.of(Map.of("config_value","{\"enabled\":true,\"writeEnabled\":true,\"updateEnabled\":true}"))).when(jdbc).queryForList(contains("FROM lin_operational_config"));
  long id=service.create(order,command());var before=service.detail(order,id);var parent=store.one("SELECT SampleStatus,SendReportDt FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order);
  doThrow(new IllegalStateException("synthetic update audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));
  var change=new LegacyOrderDetailService.UpdateCommand(test,BigDecimal.TEN,"9",BigDecimal.ONE,"changed",0,"changed",0,0,"2024-05-06","changed mail","changed report","update_"+UUID.randomUUID().toString().replace("-",""),before.get("editToken").toString());
  assertThrows(IllegalStateException.class,()->service.update(order,id,change));assertEquals(before,service.detail(order,id));
  assertEquals(parent,store.one("SELECT SampleStatus,SendReportDt FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order));
 }
}
