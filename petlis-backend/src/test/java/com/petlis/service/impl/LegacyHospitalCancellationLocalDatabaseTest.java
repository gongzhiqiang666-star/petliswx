package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@SpringBootTest @Import(LegacyBankJdbcTestConfiguration.class)
@Transactional("legacyBankTestTransactions")
@EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyHospitalCancellationLocalDatabaseTest {
 @Autowired FeatureStore store;
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired SessionService sessions;
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 LegacyHospitalHistoryLocalDatabaseTest fixture;
 LegacyHospitalCancellationService service;
 @BeforeEach void setup(){fixture=new LegacyHospitalHistoryLocalDatabaseTest();fixture.store=store;fixture.setup();fixture.gate("{\"enabled\":true,\"writeEnabled\":true,\"cancelEnabled\":true}");service=new LegacyHospitalCancellationService(store,fixture.f.access,fixture.service);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=0 WHERE ListDiagnosisProjectId=?",fixture.f.order);}
 @SuppressWarnings("unchecked") Map<String,Object> row(){return ((List<Map<String,Object>>)fixture.service.list(fixture.query(fixture.f.prefix,null,null,null,0,10)).get("items")).get(0);}
 LegacyHospitalCancellationService.Command command(){return new LegacyHospitalCancellationService.Command((int)fixture.f.hospital,(String)row().get("cancelToken"),"cancel_"+UUID.randomUUID().toString().replace("-",""));}
 @Test void sourceCancellationChangesOnlyStatusAndPreservesPaidFundsAndBodies(){
  fixture.f.detail(fixture.f.test,"8.25",1,0);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=0,IsPayMoney=1 WHERE ListDiagnosisProjectId=?",fixture.f.order);
  var before=new LinkedHashMap<>(store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",fixture.f.order));var details=store.jdbc().queryForList("SELECT * FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",fixture.f.order);var bodies=store.jdbc().queryForList("SELECT i.* FROM lin_lb_diagnosisdetail_info i JOIN lin_lb_diagnosisdetail d ON d.DiagnosisDetailId=i.diagnosisdetail_id WHERE d.ListDiagnosisProjectId=?",fixture.f.order);var hospital=store.one("SELECT * FROM lin_lb_infohospital WHERE InfoHospitalId=?",fixture.f.hospital);
  assertEquals(true,row().get("canCancel"));service.cancel(fixture.f.order,command());before.put("SampleStatus",4);assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",fixture.f.order));assertEquals(details,store.jdbc().queryForList("SELECT * FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",fixture.f.order));assertEquals(bodies,store.jdbc().queryForList("SELECT i.* FROM lin_lb_diagnosisdetail_info i JOIN lin_lb_diagnosisdetail d ON d.DiagnosisDetailId=i.diagnosisdetail_id WHERE d.ListDiagnosisProjectId=?",fixture.f.order));assertEquals(hospital,store.one("SELECT * FROM lin_lb_infohospital WHERE InfoHospitalId=?",fixture.f.hospital));assertEquals(false,row().get("canCancel"));
 }
 @Test void replayDoesNotWriteAgainAndChangedRequestCannotReuseKey(){var c=command();service.cancel(fixture.f.order,c);service.cancel(fixture.f.order,c);assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_CANCEL' AND request_id=?",Integer.class,fixture.f.actor,c.requestKey()));assertEquals(409,assertThrows(BusinessException.class,()->service.cancel(fixture.f.order,new LegacyHospitalCancellationService.Command(c.hospitalId(),"a".repeat(64),c.requestKey()))).getCode());}
 @Test void staleSnapshotScopeAndStatusAreRejected(){var c=command();assertEquals(403,assertThrows(BusinessException.class,()->service.cancel(fixture.f.order,new LegacyHospitalCancellationService.Command(2147483647,c.expectedToken(),c.requestKey()))).getCode());store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET AnimalName='changed by other writer' WHERE ListDiagnosisProjectId=?",fixture.f.order);assertEquals(409,assertThrows(BusinessException.class,()->service.cancel(fixture.f.order,c)).getCode());store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=2 WHERE ListDiagnosisProjectId=?",fixture.f.order);assertEquals(false,row().get("canCancel"));assertEquals(409,assertThrows(BusinessException.class,()->service.cancel(fixture.f.order,command())).getCode());}
 @Test void writeGateIsIndependentAndNewWorkflowCannotUseLegacyCancellation(){var c=command();fixture.gate("{\"enabled\":true,\"writeEnabled\":true,\"cancelEnabled\":\"true\"}");assertEquals(false,row().get("canCancel"));assertEquals(409,assertThrows(BusinessException.class,()->service.cancel(fixture.f.order,c)).getCode());fixture.gate("{\"enabled\":true,\"writeEnabled\":true,\"cancelEnabled\":true}");long lab=store.insert("INSERT INTO lin_laboratory(name,code,active) VALUES(?,?,1)",fixture.f.prefix,fixture.f.prefix);store.jdbc().update("INSERT INTO lin_order_extension(order_id,laboratory_id,user_id) VALUES(?,?,?)",fixture.f.order,lab,fixture.f.actor);assertEquals(false,row().get("canCancel"));assertEquals(409,assertThrows(BusinessException.class,()->service.cancel(fixture.f.order,command())).getCode());}

 @Test void auditFailureRollsBackCancellationAndRetryCanRecover(){
  var c=command();var before=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",fixture.f.order);
  var faulty=new FeatureStore(store.jdbc(),json){@Override public long insert(String sql,Object...args){if(sql.startsWith("INSERT INTO lin_audit_record"))throw new BusinessException(409,"owned audit failure");return super.insert(sql,args);}};
  var broken=new LegacyHospitalCancellationService(faulty,fixture.f.access,fixture.service);
  var tx=new org.springframework.transaction.support.TransactionTemplate(transactions);tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
  var manager=(org.springframework.transaction.support.AbstractPlatformTransactionManager)transactions;boolean previous=manager.isNestedTransactionAllowed();manager.setNestedTransactionAllowed(true);
  try{assertEquals("owned audit failure",assertThrows(BusinessException.class,()->tx.execute(status->broken.cancel(fixture.f.order,c))).getMessage());}finally{manager.setNestedTransactionAllowed(previous);}
  assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",fixture.f.order));service.cancel(fixture.f.order,c);assertEquals(4,((Number)store.one("SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",fixture.f.order).get("SampleStatus")).intValue());
 }

 @Test void realHospitalSessionCanCancelAndReplayButAnonymousAndOtherHospitalCannot()throws Exception{
  store.jdbc().update("UPDATE lin_lb_infohospital SET IsActive=1,CityId=1 WHERE InfoHospitalId=?",fixture.f.hospital);
  store.jdbc().update("UPDATE lin_user SET hospital_id=? WHERE id=?",fixture.f.hospital,fixture.f.actor);
  var c=command();String token=sessions.issue(fixture.f.actor).getToken();String path="/api/orders/hospital-history/"+fixture.f.order+"/cancel";
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).contextPath("/api").contentType("application/json").content(json.writeValueAsString(c))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  var wrong=new LegacyHospitalCancellationService.Command(2147483647,c.expectedToken(),c.requestKey());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).contextPath("/api").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(wrong))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(403));
  for(int i=0;i<2;i++)mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).contextPath("/api").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(c))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(200)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.sampleStatus").value(4));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_CANCEL' AND request_id=?",Integer.class,fixture.f.actor,c.requestKey()));
 }
}
