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
class LegacyHospitalContactAtomicLocalDatabaseTest {
 @SpyBean FeatureStore store;@MockBean AccessService access;@Autowired LegacyHospitalContactService service;int actor;long hospital;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_contact_atomic_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'联系人回滚','normal')",prefix);hospital=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,0,0)",prefix,prefix);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));var jdbc=spy(store.jdbc());doReturn(List.of(Map.of("config_value","{\"enabled\":true,\"writeEnabled\":true,\"deleteEnabled\":true}"))).when(jdbc).queryForList(contains("FROM lin_operational_config"));doReturn(jdbc).when(store).jdbc();}
 LegacyHospitalContactService.Command command(String token){return new LegacyHospitalContactService.Command(prefix,"","test@example.test","","",0,"contact_"+UUID.randomUUID().toString().replace("-",""),token);}
 @AfterEach void cleanup(){reset(store);assertEquals(prefix,store.one("SELECT HospitalName FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital).get("HospitalName"));store.jdbc().update("DELETE FROM lin_lb_infohospitalcontact WHERE InfoHospitalId=?",hospital);store.jdbc().update("DELETE FROM lin_lb_infohospital WHERE InfoHospitalId=? AND HospitalName=?",hospital,prefix);store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_CONTACT'",actor);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);}
 @Test void auditFailureRollsBackCreate(){doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));assertThrows(IllegalStateException.class,()->service.save(hospital,null,command(null)));assertEquals(0,((Number)service.list(hospital,0,20).get("total")).intValue());}
 @Test void auditFailureRollsBackUpdate(){long id=service.save(hospital,null,command(null));var before=service.detail(hospital,id);doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));var c=command(before.get("editToken").toString());var changed=new LegacyHospitalContactService.Command("changed",c.tel(),c.mail(),c.fax(),c.remark(),1,c.requestKey(),c.expectedToken());assertThrows(IllegalStateException.class,()->service.save(hospital,id,changed));assertEquals(before,service.detail(hospital,id));}
 @Test void auditFailureRollsBackDelete(){long id=service.save(hospital,null,command(null));var before=service.detail(hospital,id);doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));var c=new LegacyHospitalContactService.DeleteCommand("delete_"+UUID.randomUUID().toString().replace("-",""),before.get("editToken").toString());assertThrows(IllegalStateException.class,()->service.delete(hospital,id,c));assertEquals(before,service.detail(hospital,id));}

}
