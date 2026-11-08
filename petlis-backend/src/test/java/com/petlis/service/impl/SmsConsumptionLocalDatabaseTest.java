package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class SmsConsumptionLocalDatabaseTest {
 @Autowired FeatureStore store;@Autowired SmsVerificationService sms;@Autowired PlatformTransactionManager manager;
 long id;String mobile;
 @BeforeEach void fixture(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));mobile="199"+String.format(Locale.ROOT,"%08d",new java.security.SecureRandom().nextInt(100000000));assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_sms WHERE mobile=?",Integer.class,mobile));id=store.insert("INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register',?,'123456',0,'127.0.0.1',UNIX_TIMESTAMP())",mobile);}
 @AfterEach void cleanup(){store.jdbc().update("DELETE FROM lin_sms WHERE id=? AND mobile=?",id,mobile);}
 @Test void failedBusinessOperationKeepsCodeThenSuccessfulCommitConsumes(){var tx=new TransactionTemplate(manager);assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->{sms.consume(mobile,"register","123456");throw new IllegalStateException("业务失败");}));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_sms WHERE id=?",Integer.class,id));sms.consume(mobile,"register","123456");assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_sms WHERE id=?",Integer.class,id));assertThrows(BusinessException.class,()->sms.consume(mobile,"register","123456"));}
 @Test void wrongAttemptsSurviveOuterRollbackAndLockAfterFive(){for(int i=1;i<=5;i++){var tx=new TransactionTemplate(manager);assertThrows(BusinessException.class,()->tx.executeWithoutResult(status->sms.consume(mobile,"register","000000")));assertEquals(i,store.jdbc().queryForObject("SELECT times FROM lin_sms WHERE id=?",Integer.class,id));}assertThrows(BusinessException.class,()->sms.consume(mobile,"register","123456"));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_sms WHERE id=?",Integer.class,id));}
}
