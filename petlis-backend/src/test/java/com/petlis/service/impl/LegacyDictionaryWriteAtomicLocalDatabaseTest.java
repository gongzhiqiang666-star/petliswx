package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real committed synthetic rows and service-owned transactions; deployment flag mocked, never enabled globally. */
@SpringBootTest @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyDictionaryWriteAtomicLocalDatabaseTest {
 @SpyBean FeatureStore store;@MockBean AccessService access;
 @Autowired LegacyDictionaryService service;@Autowired PlatformTransactionManager transactions;
 int actor;String prefix;
 @BeforeEach void fixture(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));
  prefix="it_dict_write_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
  actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'字典原子性合成账号','normal')",prefix);
  when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  JdbcTemplate jdbc=spy(store.jdbc());
  doReturn(List.of(Map.of("enabledType","BOOLEAN","enabled","true","writeType","BOOLEAN","writeEnabled","true","deleteType","BOOLEAN","deleteEnabled","true"))).when(jdbc).queryForList(contains("FROM lin_operational_config"),eq("legacy-dictionaries"));
  doReturn(jdbc).when(store).jdbc();
 }
 @AfterEach void cleanup(){
  reset(store);
  if(actor>0)new TransactionTemplate(transactions).execute(status->{
   for(var row:store.jdbc().queryForList("SELECT InfoAnimalId,AnimalName FROM lin_lb_infoanimal WHERE AnimalName IN (?,?,?)",prefix,prefix+" A",prefix+" B"))store.jdbc().update("DELETE FROM lin_lb_infoanimal WHERE InfoAnimalId=? AND AnimalName=?",row.get("InfoAnimalId"),row.get("AnimalName"));
   store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_DICTIONARY'",actor);
   store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);return null;
  });
 }
 String key(){return "dict_"+UUID.randomUUID().toString().replace("-","");}
 @Test void fourCreateRetriesCommitOneRowAndOneAudit()throws Exception{
  var command=new LegacyDictionaryService.Command(prefix,"English",0,key(),null);
  var pool=Executors.newFixedThreadPool(4);var start=new CountDownLatch(1);var futures=new ArrayList<Future<Long>>();
  try{for(int i=0;i<4;i++)futures.add(pool.submit(()->{start.await();return service.create("species",command);}));start.countDown();var ids=new HashSet<Long>();for(var f:futures)ids.add(f.get(20,TimeUnit.SECONDS));assertEquals(1,ids.size());}
  finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_infoanimal WHERE AnimalName=?",Integer.class,prefix));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_DICTIONARY'",Integer.class,actor));
 }
 @Test void concurrentEditsFromSameSnapshotHaveOneSuccessAndOneConflict()throws Exception{
  long id=service.create("species",new LegacyDictionaryService.Command(prefix,"English",0,key(),null));String token=(String)service.get("species",id).get("editToken");
  var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);var futures=new ArrayList<Future<Integer>>();
  try{for(String suffix:List.of(" A"," B")){String request=key();futures.add(pool.submit(()->{start.await();try{service.update("species",id,new LegacyDictionaryService.Command(prefix+suffix,"English",1,request,token));return 200;}catch(BusinessException error){return error.getCode();}}));}start.countDown();var codes=new ArrayList<Integer>();for(var f:futures)codes.add(f.get(20,TimeUnit.SECONDS));Collections.sort(codes);assertEquals(List.of(200,409),codes);}
  finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
  assertEquals(2,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_DICTIONARY'",Integer.class,actor));
 }
 @Test void auditFailureRollsBackInsertedDictionaryRow(){
  doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));
  assertThrows(IllegalStateException.class,()->service.create("species",new LegacyDictionaryService.Command(prefix,"English",0,key(),null)));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_infoanimal WHERE AnimalName=?",Integer.class,prefix));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_DICTIONARY'",Integer.class,actor));
 }

 @Test void auditFailureRollsBackEditedDictionaryAndPreservesSnapshot(){
  long id=service.create("species",new LegacyDictionaryService.Command(prefix,"English",0,key(),null));var before=service.get("species",id);
  doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));
  assertThrows(IllegalStateException.class,()->service.update("species",id,new LegacyDictionaryService.Command(prefix+" A","Changed",1,key(),(String)before.get("editToken"))));
  var after=service.get("species",id);assertEquals(before,after);assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_DICTIONARY'",Integer.class,actor));
 }

 @Test void auditFailureRollsBackAllPhysicallyDeletedRows(){
  long first=service.create("species",new LegacyDictionaryService.Command(prefix,"English",0,key(),null));long second=service.create("species",new LegacyDictionaryService.Command(prefix+" A","English",0,key(),null));
  var a=service.get("species",first);var b=service.get("species",second);
  var command=new LegacyDictionaryService.BatchCommand(List.of(new LegacyDictionaryService.Target(first,(String)a.get("editToken")),new LegacyDictionaryService.Target(second,(String)b.get("editToken"))),null,key());
  doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));
  assertThrows(IllegalStateException.class,()->service.delete("species",command));assertEquals(a,service.get("species",first));assertEquals(b,service.get("species",second));
 }
 @Test void fourDeleteRetriesReturnOneCommittedRemovalAndAudit()throws Exception{
  long id=service.create("species",new LegacyDictionaryService.Command(prefix,"English",0,key(),null));var before=service.get("species",id);
  var command=new LegacyDictionaryService.BatchCommand(List.of(new LegacyDictionaryService.Target(id,(String)before.get("editToken"))),null,key());
  var pool=Executors.newFixedThreadPool(4);var start=new CountDownLatch(1);var futures=new ArrayList<Future<Integer>>();
  try{for(int i=0;i<4;i++)futures.add(pool.submit(()->{start.await();return service.delete("species",command);}));start.countDown();for(var f:futures)assertEquals(1,f.get(20,TimeUnit.SECONDS));}
  finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_infoanimal WHERE InfoAnimalId=?",Integer.class,id));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND action='LEGACY_DICTIONARY_DELETED'",Integer.class,actor));
 }
 @Autowired org.springframework.cache.CacheManager cacheManager;
 @Test void committedDictionaryWriteInvalidatesMiniappListCacheButRollbackDoesNot(){
  var cache=cacheManager.getCache("dictionary");cache.put("getAnimals",List.of("old"));
  service.create("species",new LegacyDictionaryService.Command(prefix,"English",0,key(),null));
  assertNull(cache.get("getAnimals"),"miniapp must see newly committed dictionary data");
  cache.put("getAnimals",List.of("keep"));
  doThrow(new IllegalStateException("synthetic audit failure")).when(store).insert(startsWith("INSERT INTO lin_audit_record"),any(Object[].class));
  assertThrows(IllegalStateException.class,()->service.create("species",new LegacyDictionaryService.Command(prefix+" failed","English",0,key(),null)));
  assertEquals(List.of("keep"),cache.get("getAnimals").get());cache.evict("getAnimals");
 }
}
