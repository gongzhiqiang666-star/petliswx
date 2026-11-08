package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyHospitalFundsAtomicLocalDatabaseTest {
 @SpyBean FeatureStore store;@MockBean AccessService access;@Autowired LegacyHospitalFundsService service;
 int actor,secondActor,hospital;String prefix;final ThreadLocal<Integer> threadActor=new ThreadLocal<>();
 @BeforeEach void setup(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_hfund_atomic_"+UUID.randomUUID().toString().replace("-","").substring(0,10);
  actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status,money) VALUES(?,'资金原子测试','normal',37.21)",prefix);
  secondActor=(int)store.insert("INSERT INTO lin_user(username,nickname,status,money) VALUES(?,'资金第二测试','normal',37.21)",prefix+"_two");
  hospital=(int)store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,0,10.00)",prefix,prefix);
  int old=store.jdbc().queryForObject("SELECT MIN(id) FROM lin_admin WHERE status='normal'",Integer.class);
  when(access.current()).thenAnswer(invocation->new CurrentActor(threadActor.get()==null?actor:threadActor.get(),Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  var jdbc=spy(store.jdbc());doReturn(List.of(Map.of("config_value",store.encode(Map.of("enabled",true,"writeEnabled",true,"adminMappings",Map.of(String.valueOf(actor),old,String.valueOf(secondActor),old)))))).when(jdbc).queryForList(contains("FROM lin_operational_config"));doReturn(jdbc).when(store).jdbc();
 }
 @AfterEach void cleanup(){threadActor.remove();reset(store);if(hospital>0){store.jdbc().update("DELETE FROM lin_infohospital_money WHERE infohospital_id=?",hospital);store.jdbc().update("DELETE FROM lin_lb_infohospital WHERE InfoHospitalId=? AND HospitalName=?",hospital,prefix);}if(actor>0){store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_FUNDS'",actor);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_FUNDS'",secondActor);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",secondActor,prefix+"_two");}}
 BigDecimal balance(){return store.jdbc().queryForObject("SELECT amount FROM lin_lb_infohospital WHERE InfoHospitalId=?",BigDecimal.class,hospital);}
 String key(){return "hfund_"+UUID.randomUUID().toString().replace("-","");}
 @Test void repeatedConcurrentCreditCommitsOneLedgerAndAudit()throws Exception{
  var command=new LegacyHospitalFundsService.Command(BigDecimal.ONE,"幂等",key());var pool=Executors.newFixedThreadPool(4);var start=new CountDownLatch(1);var futures=new ArrayList<Future<Long>>();
  try{for(int i=0;i<4;i++)futures.add(pool.submit(()->{start.await();return service.adjust(hospital,command);}));start.countDown();var ids=new HashSet<Long>();for(var f:futures)ids.add(f.get(20,TimeUnit.SECONDS));assertEquals(1,ids.size());}finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
  assertEquals(new BigDecimal("11.00"),balance());assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_infohospital_money WHERE infohospital_id=?",Integer.class,hospital));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_HOSPITAL_FUNDS'",Integer.class,actor));
 }
 @Test void concurrentDebitsCannotSpendSameAvailableBalanceTwice()throws Exception{
  var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);var futures=new ArrayList<Future<Integer>>();
  try{for(int i=0;i<2;i++){String request=key();int selectedActor=i==0?actor:secondActor;futures.add(pool.submit(()->{threadActor.set(selectedActor);start.await();try{service.adjust(hospital,new LegacyHospitalFundsService.Command(new BigDecimal("-8.00"),"并发扣款",request));return 200;}catch(BusinessException e){return e.getCode();}}));}start.countDown();var codes=new ArrayList<Integer>();for(var f:futures)codes.add(f.get(20,TimeUnit.SECONDS));Collections.sort(codes);assertEquals(List.of(200,400),codes);}finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}assertEquals(new BigDecimal("2.00"),balance());
 }
 @Test void auditFailureRollsBackBalanceAndLedger(){
  doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));
  assertThrows(IllegalStateException.class,()->service.adjust(hospital,new LegacyHospitalFundsService.Command(BigDecimal.ONE,"回滚",key())));assertEquals(new BigDecimal("10.00"),balance());assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_infohospital_money WHERE infohospital_id=?",Integer.class,hospital));
 }
 @Test void nonAdminCannotReachBalanceOrLedger(){doThrow(new BusinessException(403,"无权访问")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.summary(hospital)).getCode());assertEquals(403,assertThrows(BusinessException.class,()->service.adjust(hospital,new LegacyHospitalFundsService.Command(BigDecimal.ONE,"拒绝",key()))).getCode());assertEquals(new BigDecimal("10.00"),balance());}
}
