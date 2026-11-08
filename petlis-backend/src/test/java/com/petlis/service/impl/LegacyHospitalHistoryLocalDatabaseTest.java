package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@SpringBootTest @Import(LegacyBankJdbcTestConfiguration.class)
@Transactional("legacyBankTestTransactions")
@EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyHospitalHistoryLocalDatabaseTest {
 @Autowired FeatureStore store;
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired SessionService sessions;
 LegacyBankPaymentQuoteLocalDatabaseTest f;
 LegacyHospitalHistoryService service;
 @BeforeEach void setup(){f=new LegacyBankPaymentQuoteLocalDatabaseTest();f.store=store;f.setup();service=new LegacyHospitalHistoryService(store,f.access);gate("{\"enabled\":true}");doctor();}
 void doctor(){when(f.access.current()).thenReturn(new CurrentActor(f.actor,Set.of(PlatformRole.DOCTOR),Set.of((int)f.hospital),Set.of(),new DataScope(false,Set.of((int)f.hospital),Set.of(),null)));}
 void gate(String json){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-hospital-history',?,'owned transaction history fixture',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",json,f.actor);}
 LegacyHospitalHistoryService.Query query(String no,Integer status,String from,String to,int page,int size){return new LegacyHospitalHistoryService.Query((int)f.hospital,no,status,from,to,page,size);}
 @SuppressWarnings("unchecked") List<Map<String,Object>> rows(Map<String,Object> result){return (List<Map<String,Object>>)result.get("items");}
 long order(int status,String date,String no){long id=LegacyDetailFixtures.order(store,f.prefix+UUID.randomUUID().toString().substring(0,4),f.actor);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId=?,SampleStatus=?,CreateDt=?,UserNO=?,DoctorName='送检医生',MasterName='主人',AnimalName='动物',Remark='驳回原文' WHERE ListDiagnosisProjectId=?",f.hospital,status,date,no,id);return id;}
 @Test void historyKeepsHospitalScopeExcludesOnlyDraftAndSortsById(){
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=-2 WHERE ListDiagnosisProjectId=?",f.order);
  long a=order(0,"2025-12-01 00:00:00",f.prefix+"-old");long b=order(4,"2024-01-01 00:00:00",f.prefix+"-new-id");order(-2,"2024-01-01 00:00:00",f.prefix+"-draft");
  long other=order(0,"2024-01-01 00:00:00",f.prefix+"-other");store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId=0 WHERE ListDiagnosisProjectId=?",other);
  var before=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",b);var result=service.list(query(null,null,null,null,0,1));assertEquals(2,((Number)result.get("total")).intValue());assertEquals(b,((Number)rows(result).get(0).get("orderId")).longValue());assertEquals(a,((Number)rows(service.list(query(null,null,null,null,1,1))).get(0).get("orderId")).longValue());assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",b));
 }
 @Test void sourceNumberStatusAndInclusiveDateFiltersArePreserved(){
  order(1,"2024-02-29 23:59:59",f.prefix+"-needle");order(4,"2024-03-01 00:00:00",f.prefix+"-needle");
  var q=query(" needle ",1,"2024-02-01T00:00:00","2024-02-29T23:59:59",0,10);var rows=rows(service.list(q));assertEquals(1,rows.size());assertEquals("驳回原文",rows.get(0).get("rejectionReason"));assertEquals("审核未通过",rows.get(0).get("statusLabel"));assertEquals("送检医生",rows.get(0).get("doctorName"));assertEquals(400,assertThrows(BusinessException.class,()->service.list(query(null,null,"2024-03-01T00:00:00","2024-02-29T23:59:59",0,10))).getCode());
 }
 @Test void sourceDetailJoinIncludesExemptAndInactiveTestsWithoutLeakingFileReference(){
  f.detail(f.test,"8.00",0,1);store.jdbc().update("UPDATE lin_lb_infotesttype SET IsActive=0 WHERE InfoTestTypeId=?",f.test);store.jdbc().update("UPDATE lin_lb_diagnosisdetail SET IsSendMail=1,PDFHttpUrl='https://never-fetch.invalid/private.pdf' WHERE ListDiagnosisProjectId=?",f.order);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=2 WHERE ListDiagnosisProjectId=?",f.order);
  var row=rows(service.list(query(f.prefix,null,null,null,0,10))).get(0);assertEquals("审核通过",row.get("statusLabel"));var items=(List<?>)row.get("items");assertEquals(1,items.size());var item=(Map<?,?>)items.get(0);assertEquals("已出报告",item.get("reportState"));assertFalse(item.containsKey("PDFHttpUrl"));assertFalse(item.containsKey("reportReference"));
 }
 @Test void independentStrictGateAndHospitalRolesCannotBeBypassed(){
  gate("{\"enabled\":\"true\"}");assertEquals(false,service.capabilities().get("legacyHospitalHistory"));assertEquals(409,assertThrows(BusinessException.class,()->service.list(query(null,null,null,null,0,10))).getCode());gate("{\"enabled\":true}");assertEquals(403,assertThrows(BusinessException.class,()->service.list(new LegacyHospitalHistoryService.Query(0,null,null,null,null,0,10))).getCode());
  when(f.access.current()).thenReturn(new CurrentActor(f.actor,Set.of(PlatformRole.LAB_STAFF),Set.of((int)f.hospital),Set.of(),new DataScope(false,Set.of((int)f.hospital),Set.of(),null)));assertEquals(false,service.capabilities().get("legacyHospitalHistory"));assertEquals(403,assertThrows(BusinessException.class,()->service.list(query(null,null,null,null,0,10))).getCode());
 }

 @Test void realSessionHttpEnforcesScopeAndClosedGate() throws Exception {
  store.jdbc().update("UPDATE lin_lb_infohospital SET IsActive=1,CityId=1 WHERE InfoHospitalId=?",f.hospital);
  store.jdbc().update("UPDATE lin_user SET hospital_id=? WHERE id=?",f.hospital,f.actor);
  String token=sessions.issue(f.actor).getToken();
  var get=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/orders/hospital-history").contextPath("/api").param("hospitalId",String.valueOf(f.hospital));
  mvc.perform(get).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  mvc.perform(get.header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(200)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.hospitalId").value((int)f.hospital));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/orders/hospital-history").contextPath("/api").param("hospitalId","2147483647").header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(403));
  gate("{\"enabled\":false}");
  mvc.perform(get).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(409));
 }
 @Test void hospitalAdministratorCanUseHistoryThroughActualSecurityChain() throws Exception {
  store.jdbc().update("UPDATE lin_lb_infohospital SET IsActive=1,CityId=1 WHERE InfoHospitalId=?",f.hospital);
  store.jdbc().update("UPDATE lin_user SET hospital_id=? WHERE id=?",f.hospital,f.actor);
  store.jdbc().update("INSERT INTO lin_user_role(user_id,role_code,hospital_id,scope_type,scope_id,created_by) VALUES(?,'HOSPITAL_ADMIN',?,'HOSPITAL',?,?)",f.actor,f.hospital,f.hospital,f.actor);
  String token=sessions.issue(f.actor).getToken();
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/orders/hospital-history").contextPath("/api").param("hospitalId",String.valueOf(f.hospital)).header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(200));
 }
}
