package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class QualificationOwnershipLocalDatabaseTest {
 @Autowired FeatureStore store;@Autowired QualificationOwnershipService ownership;@Autowired PlatformTransactionManager manager;
 final List<String> resources=new ArrayList<>();
 @BeforeEach void guard(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));}
 TransactionTemplate tx(){var tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);return tx;}
 String reference(){String name=UUID.randomUUID().toString().replace("-","")+".pdf";resources.add(name);return "/files/qualifications/"+name;}
 void rolledBack(Consumer<org.springframework.transaction.TransactionStatus> action){tx().executeWithoutResult(status->{try{action.accept(status);}finally{status.setRollbackOnly();}});}
 @AfterEach void cleanup(){for(String name:resources)store.jdbc().update("DELETE FROM lin_audit_record WHERE resource_type='QUALIFICATION' AND resource_id=?",name);}
 @Test void anonymousClaimBindsOwnerAndCannotBeReused(){rolledBack(status->{
  String ref=reference();var uploaded=ownership.uploaded(ref,0);String token=uploaded.get("uploadToken");assertEquals(43,token.length());
  String metadata=store.jdbc().queryForObject("SELECT after_json FROM lin_audit_record WHERE resource_id=?",String.class,resources.get(0));assertFalse(metadata.contains(token));
  assertEquals(403,assertThrows(BusinessException.class,()->ownership.requireOwner(ref,23)).getCode());
  ownership.claim(List.of(ref),Map.of(ref,token),23);ownership.requireOwner(ref,23);
  assertEquals(403,assertThrows(BusinessException.class,()->ownership.requireOwner(ref,24)).getCode());
  assertEquals(409,assertThrows(BusinessException.class,()->ownership.claim(List.of(ref),Map.of(ref,token),24)).getCode());
 });}
 @Test void invalidExpiredMissingAndAuthenticatedUploadClaimsAreRejected(){rolledBack(status->{
  String ref=reference();String token=ownership.uploaded(ref,0).get("uploadToken");
  assertEquals(400,assertThrows(BusinessException.class,()->ownership.claim(List.of(ref),Map.of(),23)).getCode());
  assertEquals(403,assertThrows(BusinessException.class,()->ownership.claim(List.of(ref),Map.of(ref,"a".repeat(43)),23)).getCode());
  store.jdbc().update("UPDATE lin_audit_record SET after_json=JSON_SET(after_json,'$.expiresAt',1) WHERE resource_id=?",resources.get(0));
  assertEquals(403,assertThrows(BusinessException.class,()->ownership.claim(List.of(ref),Map.of(ref,token),23)).getCode());
  String owned=reference();assertFalse(ownership.uploaded(owned,23).containsKey("uploadToken"));ownership.requireOwner(owned,23);
  assertEquals(403,assertThrows(BusinessException.class,()->ownership.claim(List.of(owned),Map.of(owned,"a".repeat(43)),24)).getCode());
  String unknown=reference();assertEquals(403,assertThrows(BusinessException.class,()->ownership.claim(List.of(unknown),Map.of(unknown,"a".repeat(43)),23)).getCode());
 });}
 @Test void rollbackDoesNotConsumeClaim(){String ref=reference();String token=ownership.uploaded(ref,0).get("uploadToken");
  rolledBack(status->ownership.claim(List.of(ref),Map.of(ref,token),23));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE action='QUALIFICATION_CLAIMED' AND resource_id=?",Integer.class,resources.get(0)));
  tx().executeWithoutResult(status->ownership.claim(List.of(ref),Map.of(ref,token),24));ownership.requireOwner(ref,24);
 }
 @Test void concurrentClaimsHaveExactlyOneWinner() throws Exception {
  String ref=reference();String token=ownership.uploaded(ref,0).get("uploadToken");var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
  try{var tasks=new ArrayList<Future<Integer>>();for(int user:List.of(23,24))tasks.add(pool.submit(()->{ready.countDown();assertTrue(start.await(5,TimeUnit.SECONDS));try{tx().executeWithoutResult(status->ownership.claim(List.of(ref),Map.of(ref,token),user));return 200;}catch(BusinessException e){return e.getCode();}}));assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();var codes=new ArrayList<Integer>();for(var task:tasks)codes.add(task.get(15,TimeUnit.SECONDS));Collections.sort(codes);assertEquals(List.of(200,409),codes);
   assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE action='QUALIFICATION_CLAIMED' AND resource_id=?",Integer.class,resources.get(0)));
  }finally{pool.shutdownNow();}
 }
 @Test void onlyApprovedLegacyReferencesAreGrandfathered(){rolledBack(status->{
  String ref=reference();int user=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'资质回归','normal')","it_qual_"+UUID.randomUUID().toString().replace("-","").substring(0,12));
  store.jdbc().update("INSERT INTO lin_doctor_profile(user_id,qualification_files,review_status) VALUES(?,?,'approved')",user,store.encode(List.of(ref)));ownership.requireOwner(ref,user);
  assertThrows(BusinessException.class,()->ownership.requireOwner(ref,user+1));
  store.jdbc().update("UPDATE lin_doctor_profile SET review_status='pending' WHERE user_id=?",user);assertThrows(BusinessException.class,()->ownership.requireOwner(ref,user));
 });}
}
