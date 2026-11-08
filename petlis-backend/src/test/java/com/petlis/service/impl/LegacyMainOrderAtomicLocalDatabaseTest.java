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
class LegacyMainOrderAtomicLocalDatabaseTest {
 @SpyBean FeatureStore store;@MockBean AccessService access;@Autowired LegacyMainOrderService service;long order;int actor;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_main_atomic_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'主单回滚','normal')",prefix);order=LegacyDetailFixtures.order(store,prefix,actor);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));var jdbc=spy(store.jdbc());doReturn(List.of(Map.of("config_value","{\"enabled\":true,\"writeEnabled\":true}"))).when(jdbc).queryForList(contains("FROM lin_operational_config"));doReturn(jdbc).when(store).jdbc();}
 @AfterEach void cleanup(){reset(store);assertEquals(prefix,store.one("SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order).get("UserNO"));store.jdbc().update("DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? AND UserNO=?",order,prefix);store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_MAIN_ORDER'",actor);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);}
 @Test void auditFailureRollsBackFieldsDatesAndStatus(){var before=service.detail(order);doThrow(new IllegalStateException("synthetic main audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));var c=new LegacyMainOrderService.Command(Map.of("AnimalName","changed","CollectDt","2024-05-06","SampleStatus","2"),"main_"+UUID.randomUUID().toString().replace("-",""),before.get("editToken").toString());assertThrows(IllegalStateException.class,()->service.update(order,c));assertEquals(before,service.detail(order));}
}
