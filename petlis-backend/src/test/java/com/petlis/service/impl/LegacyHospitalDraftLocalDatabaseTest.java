package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;import org.springframework.transaction.annotation.Transactional;
import java.util.*;import static org.junit.jupiter.api.Assertions.*;
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@SpringBootTest @Import(LegacyBankJdbcTestConfiguration.class)
@Transactional("legacyBankTestTransactions") @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyHospitalDraftLocalDatabaseTest {
 @Autowired FeatureStore store;
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired SessionService sessions;LegacyHospitalHistoryLocalDatabaseTest f;LegacyHospitalDraftService service;
 @BeforeEach void setup(){f=new LegacyHospitalHistoryLocalDatabaseTest();f.store=store;f.setup();f.gate("{\"enabled\":true,\"writeEnabled\":true,\"draftEnabled\":true,\"draftWriteEnabled\":true}");service=new LegacyHospitalDraftService(store,f.f.access,f.service);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=-2 WHERE ListDiagnosisProjectId=?",f.f.order);}
 @SuppressWarnings("unchecked") Map<String,Object> fields(){return (Map<String,Object>)service.detail(f.f.order,(int)f.f.hospital).get("fields");}
 LegacyHospitalDraftService.Command command(Map<String,String> fields){return new LegacyHospitalDraftService.Command((int)f.f.hospital,fields,(String)service.detail(f.f.order,(int)f.f.hospital).get("editToken"),"draft_"+UUID.randomUUID().toString().replace("-",""));}
 @Test void onlyHospitalDraftsAreVisibleAndSubmittedOrdersCannotBeEdited(){assertEquals(1,((Number)service.list((int)f.f.hospital,0,10).get("total")).intValue());assertEquals(403,assertThrows(BusinessException.class,()->service.list(2147483647,0,10)).getCode());store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=0 WHERE ListDiagnosisProjectId=?",f.f.order);assertEquals(0,((Number)service.list((int)f.f.hospital,0,10).get("total")).intValue());assertEquals(409,assertThrows(BusinessException.class,()->service.detail(f.f.order,(int)f.f.hospital)).getCode());}
 @Test void savingPatientFieldsPreservesEveryOtherColumnAndSourceUpdateDoesNotDeriveBirthdayOrBreed(){
  f.f.detail(f.f.test,"8.25",1,0);var detailBefore=store.jdbc().queryForList("SELECT d.*,i.MailReport,i.ReportResult FROM lin_lb_diagnosisdetail d LEFT JOIN lin_lb_diagnosisdetail_info i ON i.diagnosisdetail_id=d.DiagnosisDetailId WHERE d.ListDiagnosisProjectId=?",f.f.order);
  var before=new LinkedHashMap<>(store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order));var changes=Map.of("MasterName","主人更正","AnimalName","宠物更正","Age","3岁2月","DoctorName","送检医生更正","SampleArea","前肢");var c=command(changes);service.update(f.f.order,c);before.putAll(changes);assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order));assertEquals(detailBefore,store.jdbc().queryForList("SELECT d.*,i.MailReport,i.ReportResult FROM lin_lb_diagnosisdetail d LEFT JOIN lin_lb_diagnosisdetail_info i ON i.diagnosisdetail_id=d.DiagnosisDetailId WHERE d.ListDiagnosisProjectId=?",f.f.order));assertEquals("3岁2月",fields().get("Age"));assertEquals("宠物更正",fields().get("AnimalName"));service.update(f.f.order,c);assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_DRAFT' AND request_id=?",Integer.class,f.f.actor,c.requestKey()));
 }
 @Test void staleVersionAndProtectedFieldsAndDisabledWriteAreRejected(){var c=command(Map.of("AnimalName","新名"));store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET DoctorName='other writer' WHERE ListDiagnosisProjectId=?",f.f.order);assertEquals(409,assertThrows(BusinessException.class,()->service.update(f.f.order,c)).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.update(f.f.order,command(Map.of("SampleStatus","0")))).getCode());f.gate("{\"enabled\":true,\"draftEnabled\":true,\"writeEnabled\":true,\"draftWriteEnabled\":\"true\"}");assertEquals(409,assertThrows(BusinessException.class,()->service.update(f.f.order,command(Map.of("AnimalName","不能保存")))).getCode());}
 @Test void originalSexChoicesUseZeroActiveAndExistingUnavailableSelectionIsPreserved(){
  long zero=store.insert("INSERT INTO lin_lb_dictsex(SexName,SexName_en,IsActive,createtime,updatetime) VALUES(?,'',0,100,100)",f.f.prefix+"_zero"),one=store.insert("INSERT INTO lin_lb_dictsex(SexName,SexName_en,IsActive,createtime,updatetime) VALUES(?,'',1,100,100)",f.f.prefix+"_one");var q=service.choices((int)f.f.hospital,"sex",f.f.prefix,0,10);String encoded=store.encode(q);assertTrue(encoded.contains(f.f.prefix+"_zero"));assertFalse(encoded.contains(f.f.prefix+"_one"));store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET DictSexId=? WHERE ListDiagnosisProjectId=?",one,f.f.order);assertEquals(one,((Number)fields().get("DictSexId")).longValue());service.update(f.f.order,command(Map.of("DictSexId",String.valueOf(one))));assertEquals(one,((Number)fields().get("DictSexId")).longValue());
 }

 @Test void draftReadGateAndModernWorkflowGuardAreEnforced(){
  f.gate("{\"enabled\":true,\"draftEnabled\":\"true\"}");assertEquals(409,assertThrows(BusinessException.class,()->service.list((int)f.f.hospital,0,10)).getCode());
  f.gate("{\"enabled\":true,\"writeEnabled\":true,\"draftEnabled\":true,\"draftWriteEnabled\":true}");var c=command(Map.of("AnimalName","不能修改"));
  long lab=store.insert("INSERT INTO lin_laboratory(name,code,active) VALUES(?,?,1)",f.f.prefix,f.f.prefix);store.jdbc().update("INSERT INTO lin_order_extension(order_id,laboratory_id,user_id) VALUES(?,?,?)",f.f.order,lab,f.f.actor);
  assertEquals(0,((Number)service.list((int)f.f.hospital,0,10).get("total")).intValue());assertEquals(409,assertThrows(BusinessException.class,()->service.update(f.f.order,c)).getCode());
 }
 @Test void auditFailureRollsBackFieldsAndRetryRecovers(){
  var c=command(Map.of("AnimalName","恢复后的名字"));var before=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order);
  var faulty=new FeatureStore(store.jdbc(),json){@Override public long insert(String sql,Object...args){if(sql.startsWith("INSERT INTO lin_audit_record"))throw new BusinessException(409,"owned draft audit failure");return super.insert(sql,args);}};
  var broken=new LegacyHospitalDraftService(faulty,f.f.access,f.service);var tx=new org.springframework.transaction.support.TransactionTemplate(transactions);tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
  var manager=(org.springframework.transaction.support.AbstractPlatformTransactionManager)transactions;boolean previous=manager.isNestedTransactionAllowed();manager.setNestedTransactionAllowed(true);
  try{assertThrows(BusinessException.class,()->tx.execute(status->broken.update(f.f.order,c)));}finally{manager.setNestedTransactionAllowed(previous);}
  assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order));service.update(f.f.order,c);assertEquals("恢复后的名字",fields().get("AnimalName"));
 }
 @Test void realSessionHttpCanOpenSaveReopenAndReplayWhileAnonymousAndOtherHospitalAreDenied()throws Exception{
  store.jdbc().update("UPDATE lin_lb_infohospital SET IsActive=1,CityId=1 WHERE InfoHospitalId=?",f.f.hospital);store.jdbc().update("UPDATE lin_user SET hospital_id=? WHERE id=?",f.f.hospital,f.f.actor);String token=sessions.issue(f.f.actor).getToken();String path="/api/orders/hospital-history/drafts/"+f.f.order;
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).contextPath("/api").param("hospitalId",String.valueOf(f.f.hospital))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).contextPath("/api").param("hospitalId","2147483647").header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(403));
  var c=command(Map.of("AnimalName","HTTP重开名字"));for(int i=0;i<2;i++)mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).contextPath("/api").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(c))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(200));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).contextPath("/api").param("hospitalId",String.valueOf(f.f.hospital)).header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.fields.AnimalName").value("HTTP重开名字"));
 }

 @Test void sampleDateSavesAsOriginalDayAndInvalidDateCannotOverwriteIt(){
  service.update(f.f.order,command(Map.of("CollectDt","2024-02-29")));assertEquals("2024-02-29",fields().get("CollectDt"));
  var before=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order);
  assertEquals(400,assertThrows(BusinessException.class,()->service.update(f.f.order,command(Map.of("CollectDt","2024-02-30")))).getCode());assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order));
 }
}
