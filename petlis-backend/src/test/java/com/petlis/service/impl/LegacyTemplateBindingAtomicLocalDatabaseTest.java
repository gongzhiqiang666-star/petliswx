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
class LegacyTemplateBindingAtomicLocalDatabaseTest {
 @SpyBean FeatureStore store;@MockBean AccessService access;@Autowired LegacyTemplateBindingService service;int actor;String prefix;Long id;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_binding_atomic_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'模板回滚','normal')",prefix);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));var jdbc=spy(store.jdbc());doReturn(List.of(Map.of("config_value","{\"enabled\":true,\"writeEnabled\":true,\"deleteEnabled\":true}"))).when(jdbc).queryForList(contains("FROM lin_operational_config"));doReturn(jdbc).when(store).jdbc();}
 @AfterEach void cleanup(){reset(store);if(id!=null)store.jdbc().update("DELETE FROM lin_lb_dictbindkey WHERE id=? AND DescName=?",id,prefix);store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_TEMPLATE_BINDING'",actor);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);}
 String key(){return "tpl_"+UUID.randomUUID().toString().replace("-","");}
 void failAudit(){doThrow(new IllegalStateException("synthetic template audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));}
 @Test void createRollsBackWhenAuditFails(){failAudit();assertThrows(IllegalStateException.class,()->service.save(null,new LegacyTemplateBindingService.Command("ListDiagnosisProject","UserNO",prefix,key(),null)));assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_dictbindkey WHERE DescName=?",Integer.class,prefix));}
 @Test void updateAndDeleteRollBackWhenAuditFails(){id=service.save(null,new LegacyTemplateBindingService.Command("ListDiagnosisProject","UserNO",prefix,key(),null));var before=service.detail(id);String token=before.get("editToken").toString();failAudit();assertThrows(IllegalStateException.class,()->service.save(id,new LegacyTemplateBindingService.Command("ListDiagnosisProject","AnimalName",prefix,key(),token)));assertEquals(before,service.detail(id));assertThrows(IllegalStateException.class,()->service.delete(id,new LegacyTemplateBindingService.DeleteCommand(key(),token)));assertEquals(before,service.detail(id));}
}
