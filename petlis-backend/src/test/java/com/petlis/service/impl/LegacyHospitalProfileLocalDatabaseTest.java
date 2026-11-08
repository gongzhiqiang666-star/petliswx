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
class LegacyHospitalProfileLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyHospitalProfileService service;AccessService access;long hospital;int actor;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_hprofile_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'档案测试','normal')",prefix);hospital=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount,LoginName,LoginPwd,IsActive) VALUES(?,?,0,37.21,'historical-login','historical-secret',1)",prefix,prefix);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyHospitalProfileService(store,access);gate(true,true);}
 void gate(boolean read,boolean write){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-hospital-profiles',?,'transaction-only profile test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",read,"writeEnabled",write)),actor);}
 String key(){return "profile_"+UUID.randomUUID().toString().replace("-","");}
 LegacyHospitalProfileService.Command command(String token,String key){return new LegacyHospitalProfileService.Command(" 修改医院 ","Hospital","抬头",0,"地区","详细地址","电话","传真",1,key,token);}
 @Test void explicitHospitalDisableDoesNotReviewDoctorsOrRewriteHistoricalOrders(){
  store.jdbc().update("UPDATE lin_user SET hospital_id=? WHERE id=?",hospital,actor);
  store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,review_status,credential_version) VALUES(?,'pending',7)",actor);
  long order=LegacyDetailFixtures.order(store,prefix+"_old",actor);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId=?,SampleStatus=2,TestFee=12.34,IsPayMoney=0 WHERE ListDiagnosisProjectId=?",hospital,order);
  var historical=store.one("SELECT SampleStatus,TestFee,IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order);
  var c=command(service.detail(hospital).get("editToken").toString(),key());
  service.update(hospital,new LegacyHospitalProfileService.Command(c.name(),c.englishName(),c.invoice(),c.cityId(),c.place(),c.address(),c.tel(),c.fax(),c.payType(),c.requestKey(),c.expectedToken(),0));
  assertEquals(0,((Number)service.detail(hospital).get("IsActive")).intValue());assertEquals(historical,store.one("SELECT SampleStatus,TestFee,IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order));
  var doctor=store.one("SELECT review_status,credential_version FROM lin_doctor_profile WHERE user_id=?",actor);assertEquals("pending",doctor.get("review_status"));assertEquals(7,((Number)doctor.get("credential_version")).intValue());
 }
 @Test void credentialMaintenanceKeepsBlankClearsExplicitlyAndNeverReturnsSecrets(){
  var before=service.credentials(hospital);assertEquals("historical-login",before.get("loginName"));assertEquals(true,before.get("passwordConfigured"));assertFalse(before.containsKey("password"));
  var keep=new LegacyHospitalProfileService.CredentialsCommand("renamed",null,false,key(),before.get("editToken").toString());
  assertEquals(409,assertThrows(BusinessException.class,()->service.updateCredentials(hospital,keep)).getCode());
  store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-hospital-profiles' AND scope_type='GLOBAL' AND scope_id=0",store.encode(Map.of("enabled",true,"writeEnabled",true,"credentialWriteEnabled",true)));
  service.updateCredentials(hospital,keep);assertEquals(hospital,service.updateCredentials(hospital,keep));assertEquals("historical-secret",store.one("SELECT LoginPwd FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital).get("LoginPwd"));
  var current=service.credentials(hospital);var clear=new LegacyHospitalProfileService.CredentialsCommand("renamed","",true,key(),current.get("editToken").toString());service.updateCredentials(hospital,clear);assertEquals(false,service.credentials(hospital).get("passwordConfigured"));
  var change=new LegacyHospitalProfileService.CredentialsCommand("renamed","synthetic-new-secret",false,key(),service.credentials(hospital).get("editToken").toString());service.updateCredentials(hospital,change);
  String audit=store.jdbc().queryForList("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND action='LEGACY_HOSPITAL_CREDENTIALS_UPDATED'",actor).toString();assertFalse(audit.contains("synthetic-new-secret"));assertFalse(audit.contains("historical-secret"));assertEquals("37.21",store.one("SELECT amount FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital).get("amount").toString());
 }
 @Test void createUsesOriginalDefaultsAndRedactsCredentialsFromAudit(){
  var c=new LegacyHospitalProfileService.CreateCommand(command(null,key()),"new-legacy-login","new-legacy-secret",null);
  assertEquals(409,assertThrows(BusinessException.class,()->service.create(c)).getCode());
  store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-hospital-profiles' AND scope_type='GLOBAL' AND scope_id=0",store.encode(Map.of("enabled",true,"writeEnabled",true,"createEnabled",true)));
  long id=service.create(c);assertEquals(id,service.create(c));
  var row=store.one("SELECT amount,CreateDt,IsActive,LoginName,LoginPwd FROM lin_lb_infohospital WHERE InfoHospitalId=?",id);
  assertEquals("0.00",row.get("amount").toString());assertNull(row.get("CreateDt"));assertEquals(0,((Number)row.get("IsActive")).intValue());assertEquals("new-legacy-secret",row.get("LoginPwd"));
  String audit=store.one("SELECT after_json FROM lin_audit_record WHERE actor_user_id=? AND action='LEGACY_HOSPITAL_PROFILE_CREATED'",actor).get("after_json").toString();assertFalse(audit.contains("new-legacy-secret"));assertFalse(audit.contains("new-legacy-login"));
 }
 @Test void updateAndReplayPreserveFundsCredentialsAndStatus(){var before=service.detail(hospital);assertFalse(before.containsKey("LoginPwd"));var c=command(before.get("editToken").toString(),key());assertEquals(hospital,service.update(hospital,c));assertEquals(hospital,service.update(hospital,c));var after=service.detail(hospital);assertEquals(" 修改医院 ",after.get("HospitalName"));assertEquals(1,((Number)after.get("PayType")).intValue());var protectedRow=store.one("SELECT amount,LoginName,LoginPwd,IsActive FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital);assertEquals("37.21",protectedRow.get("amount").toString());assertEquals("historical-login",protectedRow.get("LoginName"));assertEquals("historical-secret",protectedRow.get("LoginPwd"));assertEquals(1,((Number)protectedRow.get("IsActive")).intValue());assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_PROFILE'",Integer.class,actor));}
 @Test void staleTokenAndReusedRequestCannotOverwrite(){String token=service.detail(hospital).get("editToken").toString(),key=key();service.update(hospital,command(token,key));assertEquals(409,assertThrows(BusinessException.class,()->service.update(hospital,command(token,key()))).getCode());var c=command(token,key);var changed=new LegacyHospitalProfileService.Command("other",c.englishName(),c.invoice(),c.cityId(),c.place(),c.address(),c.tel(),c.fax(),c.payType(),key,token);assertEquals(409,assertThrows(BusinessException.class,()->service.update(hospital,changed)).getCode());}
 @Test void flagsAndAuthorizationAreEnforced(){gate(true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.update(hospital,command("a".repeat(64),key()))).getCode());gate(false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.detail(hospital)).getCode());doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.capabilities()).getCode());}
}
