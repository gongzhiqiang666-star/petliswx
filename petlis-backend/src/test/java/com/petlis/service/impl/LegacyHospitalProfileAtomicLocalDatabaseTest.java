package com.petlis.service.impl;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@SpringBootTest @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyHospitalProfileAtomicLocalDatabaseTest {
 @SpyBean FeatureStore store;@MockBean AccessService access;@Autowired LegacyHospitalProfileService service;long hospital;int actor;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_hprofile_atomic_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'档案回滚','normal')",prefix);hospital=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,0,37.21)",prefix,prefix);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));var jdbc=spy(store.jdbc());doReturn(List.of(Map.of("config_value","{\"enabled\":true,\"writeEnabled\":true,\"createEnabled\":true}"))).when(jdbc).queryForList(contains("FROM lin_operational_config"));doReturn(jdbc).when(store).jdbc();}
 @AfterEach void cleanup(){reset(store);assertEquals(prefix,store.one("SELECT HospitalName FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital).get("HospitalName"));store.jdbc().update("DELETE FROM lin_lb_infohospital WHERE InfoHospitalId=? AND HospitalName=?",hospital,prefix);store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_PROFILE'",actor);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);}
 @Test void auditFailureRollsBackProfileAndSettlement(){var before=service.detail(hospital);doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));var command=new LegacyHospitalProfileService.Command("changed","English","invoice",0,"place","address","tel","fax",1,"profile_"+UUID.randomUUID().toString().replace("-",""),before.get("editToken").toString());assertThrows(IllegalStateException.class,()->service.update(hospital,command));assertEquals(before,service.detail(hospital));assertEquals("37.21",store.one("SELECT amount FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital).get("amount").toString());}
 @Test void createAuditFailureLeavesNoNewHospital(){
  doThrow(new IllegalStateException("synthetic create audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));
  var profile=new LegacyHospitalProfileService.Command(prefix+"_new","English","invoice",0,"place","address","tel","fax",0,"create_"+UUID.randomUUID().toString().replace("-",""),null);
  assertThrows(IllegalStateException.class,()->service.create(new LegacyHospitalProfileService.CreateCommand(profile,"synthetic-login","synthetic-secret",null)));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_infohospital WHERE HospitalName=?",Integer.class,prefix+"_new"));
 }

}
