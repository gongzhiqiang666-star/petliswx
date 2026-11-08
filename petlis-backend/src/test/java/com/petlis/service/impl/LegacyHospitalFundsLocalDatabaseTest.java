package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @Transactional
@EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyHospitalFundsLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyHospitalFundsService service;int actor,legacyAdmin,hospital;BigDecimal personal;String prefix;
 @BeforeEach void setup(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));
  prefix="it_hfund_"+UUID.randomUUID().toString().replace("-","").substring(0,10);
  actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status,money) VALUES(?,'医院资金测试','normal',37.21)",prefix);
  hospital=(int)store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,0,10.00)",prefix,prefix);
  legacyAdmin=store.jdbc().queryForObject("SELECT MIN(id) FROM lin_admin WHERE status='normal'",Integer.class);
  var access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  service=new LegacyHospitalFundsService(store,access);gate(true,true,true);
 }
 void gate(boolean enabled,boolean write,boolean mapping){
  String config=store.encode(Map.of("enabled",enabled,"writeEnabled",write,"adminMappings",mapping?Map.of(String.valueOf(actor),legacyAdmin):Map.of()));
  store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-hospital-funds',?,'transaction-only hospital funds test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",config,actor);
 }
 String key(){return "hfund_"+UUID.randomUUID().toString().replace("-","");}
 BigDecimal balance(){return store.jdbc().queryForObject("SELECT amount FROM lin_lb_infohospital WHERE InfoHospitalId=?",BigDecimal.class,hospital);}
 @Test void creditsAndDebitsWriteOldLedgerButNeverPersonalWallet(){
  var c=new LegacyHospitalFundsService.Command(new BigDecimal("2.50"),"加款测试",key());long id=service.adjust(hospital,c);assertEquals(id,service.adjust(hospital,c));
  assertEquals(new BigDecimal("12.50"),balance());service.adjust(hospital,new LegacyHospitalFundsService.Command(new BigDecimal("-12.50"),"扣款测试",key()));assertEquals(new BigDecimal("0.00"),balance());
  assertEquals(new BigDecimal("37.21"),store.jdbc().queryForObject("SELECT money FROM lin_user WHERE id=?",BigDecimal.class,actor));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_user_money_log WHERE user_id=?",Integer.class,actor));
  assertEquals(2,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_infohospital_money WHERE infohospital_id=?",Integer.class,hospital));
  assertEquals(legacyAdmin,store.jdbc().queryForObject("SELECT admin_id FROM lin_infohospital_money WHERE id=?",Integer.class,id));
 }
 @Test void overdraftZeroAndKeyReuseDoNotChangeBalance(){
  assertThrows(BusinessException.class,()->service.adjust(hospital,new LegacyHospitalFundsService.Command(new BigDecimal("-10.01"),"不足",key())));
  assertThrows(BusinessException.class,()->service.adjust(hospital,new LegacyHospitalFundsService.Command(BigDecimal.ZERO,"零",key())));assertEquals(new BigDecimal("10.00"),balance());
  String request=key();service.adjust(hospital,new LegacyHospitalFundsService.Command(BigDecimal.ONE,"一次",request));
  assertEquals(409,assertThrows(BusinessException.class,()->service.adjust(hospital,new LegacyHospitalFundsService.Command(new BigDecimal("2"),"一次",request))).getCode());assertEquals(new BigDecimal("11.00"),balance());
 }
 @Test void gatesAndMissingLegacyIdentityCannotSilentlyAttributeToNewUserId(){
  gate(true,true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.adjust(hospital,new LegacyHospitalFundsService.Command(BigDecimal.ONE,"mapping",key()))).getCode());
  gate(true,false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.adjust(hospital,new LegacyHospitalFundsService.Command(BigDecimal.ONE,"readonly",key()))).getCode());
  gate(false,true,true);assertEquals(409,assertThrows(BusinessException.class,()->service.summary(hospital)).getCode());assertEquals(new BigDecimal("10.00"),balance());
 }
 @Test void readIsScopedToHospitalAndDoesNotExposeHospitalCredentials(){
  service.adjust(hospital,new LegacyHospitalFundsService.Command(BigDecimal.ONE,"查看",key()));
  assertEquals(1,((Number)service.entries(hospital,0,20).get("total")).intValue());
  var summary=service.summary(hospital);assertFalse(summary.containsKey("LoginPwd"));assertEquals("HOSPITAL",summary.get("fundPool"));
 }
 @Test void invalidPrecisionAndOverflowCannotPartiallyWrite(){
  assertThrows(BusinessException.class,()->service.adjust(hospital,new LegacyHospitalFundsService.Command(new BigDecimal("0.009"),"精度",key())));
  assertThrows(BusinessException.class,()->service.adjust(hospital,new LegacyHospitalFundsService.Command(new BigDecimal("99999999.99"),"溢出",key())));assertEquals(new BigDecimal("10.00"),balance());
 }
}
