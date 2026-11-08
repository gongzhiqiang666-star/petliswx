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
class LegacyPaymentLogAtomicLocalDatabaseTest {
 @SpyBean FeatureStore store;@MockBean AccessService access;@Autowired LegacyPaymentLogService service;int actor;String prefix;Long id;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_paylog_atomic_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'流水回滚','normal')",prefix);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));var jdbc=spy(store.jdbc());doReturn(List.of(Map.of("config_value","{\"enabled\":true,\"writeEnabled\":true,\"deleteEnabled\":true}"))).when(jdbc).queryForList(contains("FROM lin_operational_config"));doReturn(jdbc).when(store).jdbc();}
 @AfterEach void cleanup(){reset(store);if(id!=null)store.jdbc().update("DELETE FROM lin_lb_paylog WHERE PayLogId=? AND TradeNoUser=?",id,prefix);store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_PAYMENT_LOG'",actor);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);}
 String key(){return "paylog_"+UUID.randomUUID().toString().replace("-","");}
 LegacyPaymentLogService.Command command(String money,String token){var f=new LinkedHashMap<String,String>();for(String k:List.of("BuyMail","BuyerId","TradeStatus","Subject","TradeBody","Exterface"))f.put(k,"");f.put("TradeNoAli",prefix);f.put("TradeNoUser",prefix);f.put("TotalFee",money);f.put("CreateDt","2020-02-29 13:14:15");return new LegacyPaymentLogService.Command(f,key(),token);}
 void failAudit(){doThrow(new IllegalStateException("synthetic payment log audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));}
 @Test void createRollsBackWhenAuditFails(){failAudit();assertThrows(IllegalStateException.class,()->service.save(null,command("1",null)));assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser=?",Integer.class,prefix));}
 @Test void updateAndDeleteRollBackWhenAuditFails(){id=service.save(null,command("1",null));var before=service.detail(id);String token=before.get("editToken").toString();failAudit();assertThrows(IllegalStateException.class,()->service.save(id,command("2",token)));assertEquals(before,service.detail(id));assertThrows(IllegalStateException.class,()->service.delete(id,new LegacyPaymentLogService.DeleteCommand(key(),token)));assertEquals(before,service.detail(id));}
}
