package com.petlis.service.impl;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import java.util.*;import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@SpringBootTest @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyFinanceAtomicLocalDatabaseTest {
 @SpyBean FeatureStore store;@MockBean AccessService access;@Autowired LegacyFinanceService service;@Autowired LegacyOrderDetailService details;int actor,test;long order;String prefix;List<Long> ids=new ArrayList<>();
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_fin_atomic_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'财务回滚','normal')",prefix);order=LegacyDetailFixtures.order(store,prefix,actor);test=LegacyDetailFixtures.test(store,prefix);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));var jdbc=spy(store.jdbc());doReturn(List.of(Map.of("config_value","{\"enabled\":true,\"writeEnabled\":true}"))).when(jdbc).queryForList(contains("FROM lin_operational_config"));doReturn(jdbc).when(store).jdbc();for(int i=0;i<2;i++)ids.add(details.create(order,new LegacyOrderDetailService.CreateCommand(test,BigDecimal.ONE,"10",BigDecimal.ONE,prefix,0,"",0,0,0,0,"mail","report",key())));}
 String key(){return "finance_"+UUID.randomUUID().toString().replace("-","");}
 @AfterEach void cleanup(){reset(store);for(long id:ids){store.jdbc().update("DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id=?",id);store.jdbc().update("DELETE FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=? AND ListDiagnosisProjectId=?",id,order);}store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type IN ('LEGACY_FINANCE_BATCH','LEGACY_ORDER_DETAIL')",actor);store.jdbc().update("DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? AND UserNO=?",order,prefix);store.jdbc().update("DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId=? AND TestName=?",test,prefix);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);}
 @Test void failedAuditRollsBackEverySelectedFlag(){var before=ids.stream().map(service::detail).toList();var targets=before.stream().map(row->new LegacyFinanceService.Target(((Number)row.get("DiagnosisDetailId")).longValue(),row.get("editToken").toString())).toList();doThrow(new IllegalStateException("synthetic finance audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));assertThrows(IllegalStateException.class,()->service.batch(new LegacyFinanceService.BatchCommand(targets,"is_invoiced",1,key())));assertEquals(before,ids.stream().map(service::detail).toList());assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_FINANCE_BATCH'",Integer.class,actor));}
}
