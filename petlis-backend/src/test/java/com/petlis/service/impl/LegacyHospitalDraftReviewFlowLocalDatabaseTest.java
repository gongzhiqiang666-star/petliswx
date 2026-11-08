package com.petlis.service.impl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Actual production JPA transaction manager, JWT filter and review services; all fixtures roll back. */
@SpringBootTest @AutoConfigureMockMvc @Transactional("transactionManager")
@EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyHospitalDraftReviewFlowLocalDatabaseTest {
 @Autowired FeatureStore store;@Autowired ObjectMapper json;@Autowired MockMvc mvc;@Autowired SessionService sessions;
 JsonNode getData(String path,String token,String...pairs)throws Exception{var request=get("/api"+path).contextPath("/api").header("Authorization","Bearer "+token);for(int i=0;i<pairs.length;i+=2)request.param(pairs[i],pairs[i+1]);var result=mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn();return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("data");}
 JsonNode postData(String path,String token,Object body)throws Exception{var result=mvc.perform(post("/api"+path).contextPath("/api").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn();return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("data");}
 @Test void oldDraftCanSubmitThenExplicitAdministratorNumberBindAndApproveWithoutRepricingDetails()throws Exception{
  var f=new LegacyHospitalHistoryLocalDatabaseTest();f.store=store;f.setup();f.gate("{\"enabled\":true,\"writeEnabled\":true,\"draftEnabled\":true,\"draftWriteEnabled\":true,\"draftItemsEnabled\":true,\"draftSubmitEnabled\":true,\"previewVarietyMode\":\"LEGACY\"}");
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET SampleStatus=-2,CreateSampleId=0,UserNO='',TestFee=0 WHERE ListDiagnosisProjectId=?",f.f.order);
  store.jdbc().update("UPDATE lin_lb_infohospital SET IsActive=1,CityId=1,PayType=1 WHERE InfoHospitalId=?",f.f.hospital);store.jdbc().update("UPDATE lin_user SET hospital_id=? WHERE id=?",f.f.hospital,f.f.actor);
  store.jdbc().update("UPDATE lin_lb_infotesttype SET type=1 WHERE InfoTestTypeId=?",f.f.test);store.insert("INSERT INTO lin_lb_infotesttype_price(InfoTestTypeid,price,IsActive) VALUES(?,8.25,1)",f.f.test);
  long admin=store.insert("INSERT INTO lin_user(username,nickname,status,hospital_id) VALUES(?,'合成审核员','normal',NULL)",f.f.prefix+"_admin");store.jdbc().update("INSERT INTO lin_user_role(user_id,role_code,scope_type,scope_id,created_by) VALUES(?,'ADMIN','GLOBAL',0,?)",admin,admin);
  long lab=store.insert("INSERT INTO lin_laboratory(name,code,active) VALUES(?,?,1)",f.f.prefix,f.f.prefix);store.jdbc().update("INSERT INTO lin_lab_test(laboratory_id,test_type_id,price,active) VALUES(?,?,8.25,1)",lab,f.f.test);
  String doctor=sessions.issue(f.f.actor).getToken(),reviewer=sessions.issue((int)admin).getToken(),base="/orders/hospital-history/drafts/"+f.f.order,hospital=String.valueOf(f.f.hospital);
  var selected=getData(base+"/items",doctor,"hospitalId",hospital);postData(base+"/items/add",doctor,Map.of("hospitalId",f.f.hospital,"testId",f.f.test,"expectedToken",selected.path("editToken").asText(),"requestKey",f.f.prefix+"_add"));
  var preview=getData(base+"/preview",doctor,"hospitalId",hospital);assertEquals(1,preview.path("items").size());var command=Map.of("hospitalId",f.f.hospital,"expectedToken",preview.path("editToken").asText(),"requestKey",f.f.prefix+"_submit");postData(base+"/submit",doctor,command);postData(base+"/submit",doctor,command);
  var pending=store.one("SELECT SampleStatus,CreateSampleId,UserNO,TestFee FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order);assertEquals(0,((Number)pending.get("SampleStatus")).intValue());assertEquals(0,((Number)pending.get("CreateSampleId")).intValue());assertEquals("",pending.get("UserNO"));assertEquals(0,new java.math.BigDecimal(pending.get("TestFee").toString()).signum());
  var details=store.jdbc().queryForList("SELECT * FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",f.f.order);var hospitalBefore=store.one("SELECT * FROM lin_lb_infohospital WHERE InfoHospitalId=?",f.f.hospital);
  store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-main-orders','{\"enabled\":true,\"writeEnabled\":true}','owned flow fixture',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",admin);
  var main=getData("/admin/legacy/main-orders/"+f.f.order,reviewer);String number=f.f.prefix+"_barcode";
  mvc.perform(put("/api/admin/legacy/main-orders/"+f.f.order).contextPath("/api").header("Authorization","Bearer "+reviewer).contentType("application/json").content(json.writeValueAsString(Map.of("fields",Map.of("UserNO",number),"expectedToken",main.path("editToken").asText(),"requestKey",f.f.prefix+"_number")))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
  postData("/legacy/orders/"+f.f.order+"/submit",reviewer,Map.of("doctorId",f.f.actor,"laboratoryId",lab,"confirmedAmount","8.25"));
  postData("/review/orders/"+f.f.order,reviewer,Map.of("approved",true,"reason","合成旧草稿接续审核"));
  var after=store.one("SELECT SampleStatus,CreateSampleId,UserNO,TestFee FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.f.order);assertEquals(2,((Number)after.get("SampleStatus")).intValue());assertEquals(number,after.get("UserNO"));assertEquals(f.f.actor,((Number)after.get("CreateSampleId")).intValue());assertEquals(0,new java.math.BigDecimal("8.25").compareTo(new java.math.BigDecimal(after.get("TestFee").toString())));
  assertEquals(details,store.jdbc().queryForList("SELECT * FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",f.f.order));assertEquals(hospitalBefore,store.one("SELECT * FROM lin_lb_infohospital WHERE InfoHospitalId=?",f.f.hospital));assertEquals("approved",store.one("SELECT review_status FROM lin_order_extension WHERE order_id=?",f.f.order).get("review_status"));
 }
}
