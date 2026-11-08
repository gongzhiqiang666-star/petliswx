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
 @BeforeEach void setup(){f=new LegacyHospitalHistoryLocalDatabaseTest();f.store=store;f.setup();f.gate("{\"enabled\":true,\"writeEnabled\":true,\"draftEnabled\":true,\"draftWriteEnabled\":true,\"draftCreateEnabled\":true}");store.jdbc().update("UPDATE lin_lb_infohospital SET IsActive=1 WHERE InfoHospitalId=?",f.f.hospital);service=new LegacyHospitalDraftService(store,f.f.access,f.service);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=-2 WHERE ListDiagnosisProjectId=?",f.f.order);}
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
  var c=command(Map.of("AnimalName","HTTP重开名字","Remark","<p><strong>HTTP原生备注</strong></p>"));for(int i=0;i<2;i++)mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).contextPath("/api").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(c))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(200));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).contextPath("/api").param("hospitalId",String.valueOf(f.f.hospital)).header("Authorization","Bearer "+token)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.fields.AnimalName").value("HTTP重开名字")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.fields.Remark").value("<p><strong>HTTP原生备注</strong></p>"));
 }

 @Test void sampleDateSavesAsOriginalDayAndInvalidDateCannotOverwriteIt(){
  service.update(f.f.order,command(Map.of("CollectDt","2024-02-29")));assertEquals("2024-02-29",fields().get("CollectDt"));
  var before=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order);
  assertEquals(400,assertThrows(BusinessException.class,()->service.update(f.f.order,command(Map.of("CollectDt","2024-02-30")))).getCode());assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order));
 }
 @Test void hospitalCreationKeepsSourceDefaultsAndReplaysWithoutSecondOrder(){
  var c=new LegacyHospitalDraftService.CreateCommand((int)f.f.hospital,Map.of("MasterName","新主人","AnimalName","新动物","Age","2岁3月","DoctorName","原送检医师","SampleArea","创建会清空","Remark","<p>创建会清空</p>"),"create_"+UUID.randomUUID().toString().replace("-",""));
  long id=service.create(c);assertEquals(id,service.create(c));var row=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",id);
  assertEquals(-2,((Number)row.get("SampleStatus")).intValue());assertEquals(2,((Number)row.get("CreateDataTypeIId")).intValue());assertEquals(f.f.hospital,((Number)row.get("InfoHospitalId")).longValue());
  assertEquals("新主人",row.get("MasterName_en"));assertEquals("",row.get("UserNO"));assertEquals("",row.get("SampleArea"));assertEquals("",row.get("Remark"));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_order_extension WHERE order_id=?",Integer.class,id));assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",Integer.class,id));
  assertEquals("新动物",((Map<?,?>)service.detail(id,(int)f.f.hospital).get("fields")).get("AnimalName"));
 }
 @Test void createRequiresIndependentGateHospitalScopeAndCannotReplayDifferentContent(){
  var c=new LegacyHospitalDraftService.CreateCommand((int)f.f.hospital,Map.of("Age","1岁0月"),"create_"+UUID.randomUUID().toString().replace("-",""));long id=service.create(c);
  assertEquals(409,assertThrows(BusinessException.class,()->service.create(new LegacyHospitalDraftService.CreateCommand(c.hospitalId(),Map.of("Age","2岁0月"),c.requestKey()))).getCode());
  assertEquals(403,assertThrows(BusinessException.class,()->service.create(new LegacyHospitalDraftService.CreateCommand(2147483647,c.fields(),c.requestKey()))).getCode());
  f.gate("{\"enabled\":true,\"writeEnabled\":true,\"draftEnabled\":true,\"draftWriteEnabled\":true}");assertEquals(409,assertThrows(BusinessException.class,()->service.create(c)).getCode());assertNotNull(service.detail(id,(int)f.f.hospital));
 }

 @Test void createAuditFailureRollsBackParentAndSameRequestCanRecover(){
  var c=new LegacyHospitalDraftService.CreateCommand((int)f.f.hospital,Map.of("Age","1岁0月","AnimalName",f.f.prefix+"_create"),"create_"+UUID.randomUUID().toString().replace("-",""));
  var before=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_listdiagnosisproject WHERE InfoHospitalId=?",Integer.class,f.f.hospital);
  var faulty=new FeatureStore(store.jdbc(),json){@Override public long insert(String sql,Object...args){if(sql.startsWith("INSERT INTO lin_audit_record"))throw new BusinessException(409,"owned creation audit failure");return super.insert(sql,args);}};
  var broken=new LegacyHospitalDraftService(faulty,f.f.access,f.service);var tx=new org.springframework.transaction.support.TransactionTemplate(transactions);tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);var manager=(org.springframework.transaction.support.AbstractPlatformTransactionManager)transactions;boolean previous=manager.isNestedTransactionAllowed();manager.setNestedTransactionAllowed(true);
  try{assertThrows(BusinessException.class,()->tx.execute(status->broken.create(c)));}finally{manager.setNestedTransactionAllowed(previous);}
  assertEquals(before,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_listdiagnosisproject WHERE InfoHospitalId=?",Integer.class,f.f.hospital));long id=service.create(c);assertEquals(id,service.create(c));
 }

 @Test void hospitalRemarkHtmlSavesReopensReplaysAndCanBeClearedWithoutOtherChanges(){
  String html="<p><strong>冷藏样本</strong>当天寄出</p><table><tbody><tr><td>原表格</td></tr></tbody></table>";var before=new LinkedHashMap<>(store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order));var c=command(Map.of("Remark",html));service.update(f.f.order,c);service.update(f.f.order,c);before.put("Remark",html);assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order));assertEquals(html,fields().get("Remark"));
  service.update(f.f.order,command(Map.of("Remark","")));before.put("Remark","");assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order));assertEquals("",fields().get("Remark"));
 }

 @Test void draftHospitalPickerListsOnlyAccessibleHospitalsAndKeepsInactiveHistory(){
  store.jdbc().update("UPDATE lin_lb_infohospital SET HospitalName=?,IsActive=0 WHERE InfoHospitalId=?",f.f.prefix+"_own",f.f.hospital);
  store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount,IsActive,CityId) VALUES(?,?,1,0,1,1)",f.f.prefix+"_foreign",f.f.prefix);
  var result=f.service.hospitals(f.f.prefix,0,20);assertEquals(1,((Number)result.get("total")).intValue());var item=(Map<?,?>)((java.util.List<?>)result.get("items")).get(0);assertEquals(f.f.hospital,((Number)item.get("id")).longValue());assertEquals(f.f.prefix+"_own",item.get("name"));
  assertEquals(400,assertThrows(BusinessException.class,()->f.service.hospitals("",-1,20)).getCode());f.gate("{\"enabled\":false,\"draftEnabled\":false}");assertEquals(409,assertThrows(BusinessException.class,()->f.service.hospitals("",0,20)).getCode());
 }

 @Test void hospitalPickerWorksWithHistoryOnlyWhileDraftsRemainClosed(){f.gate("{\"enabled\":true,\"draftEnabled\":false}");assertEquals(1,((Number)f.service.hospitals("",0,20).get("total")).intValue());assertEquals(false,f.service.capabilities().get("legacyHospitalDrafts"));assertEquals(409,assertThrows(BusinessException.class,()->service.list((int)f.f.hospital,0,20)).getCode());}

}
