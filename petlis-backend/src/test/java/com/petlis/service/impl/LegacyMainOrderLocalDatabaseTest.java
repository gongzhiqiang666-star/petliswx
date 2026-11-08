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
class LegacyMainOrderLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyMainOrderService service;AccessService access;int actor;long order;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));String prefix="it_mainorder_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'主单测试','normal')",prefix);order=LegacyDetailFixtures.order(store,prefix,actor);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET CollectDt='2024-05-06',TestFee=125.50,IsPayMoney=1 WHERE ListDiagnosisProjectId=?",order);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyMainOrderService(store,access);gate(true,true);}
 void gate(boolean read,boolean write){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-main-orders',?,'transaction-only main order test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",read,"writeEnabled",write)),actor);}
 String key(){return "main_"+UUID.randomUUID().toString().replace("-","");}
 LegacyMainOrderService.Command command(Map<String,String> values){return new LegacyMainOrderService.Command(values,key(),service.detail(order).get("editToken").toString());}
 @Test void editPreservesEmptyDatesFinancialFieldsAndReplays(){var before=service.detail(order);var c=command(Map.of("AnimalName","修改宠物","MasterName","修改主人","CollectDt","","Birthday","0"));assertEquals(order,service.update(order,c));assertEquals(order,service.update(order,c));var row=service.detail(order);assertEquals("修改宠物",row.get("AnimalName"));assertEquals(before.get("CollectDt"),row.get("CollectDt"));assertEquals(before.get("Birthday"),row.get("Birthday"));assertEquals(before.get("TestFee"),row.get("TestFee"));assertEquals(before.get("IsPayMoney"),row.get("IsPayMoney"));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_MAIN_ORDER'",Integer.class,actor));}
 @Test void staleSnapshotsAndUnlistedFieldsAreRejected(){var c=command(Map.of("AnimalName","new"));service.update(order,c);assertEquals(409,assertThrows(BusinessException.class,()->service.update(order,new LegacyMainOrderService.Command(Map.of("MasterName","other"),key(),c.expectedToken()))).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.update(order,command(Map.of("TestFee","0")))).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.update(order,command(Map.of("AnimalName;DROP TABLE lin_user","x")))).getCode());}
 @Test void closedFlagsAndNonAdminAreRejected(){gate(true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.update(order,new LegacyMainOrderService.Command(Map.of("AnimalName","x"),key(),"a".repeat(64)))).getCode());gate(false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.detail(order)).getCode());doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.capabilities()).getCode());}
}
