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
class HospitalWalletLocalDatabaseTest {
 @Autowired FeatureStore store;AccessService access;HospitalWalletService service;int hospital;String prefix;
 @BeforeEach void setup(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_hwallet_"+UUID.randomUUID().toString().substring(0,8);
  hospital=(int)store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,0,12.50)",prefix,prefix);
  access=mock(AccessService.class);service=new HospitalWalletService(store,access);when(access.managedHospitalIds()).thenReturn(Set.of(hospital));
  store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-hospital-funds','{\"enabled\":true}','wallet transaction-only test',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'");
 }
 @Test void onlyManagedHospitalBalanceIsReturnedWithoutCredentials(){var rows=service.balances();assertEquals(1,rows.size());assertEquals(hospital,((Number)rows.get(0).get("hospitalId")).intValue());assertEquals(Set.of("hospitalId","hospitalName","balance","fundPool"),rows.get(0).keySet());}
 @Test void NoManagedHospitalMeansNoBalanceAndForeignLedgerDenied(){when(access.managedHospitalIds()).thenReturn(Set.of());assertTrue(service.balances().isEmpty());assertEquals(403,assertThrows(BusinessException.class,()->service.transactions(hospital,0,20,"all")).getCode());}
 @Test void GateMustBeBooleanAndCannotBeBypassedViaLedger(){store.jdbc().update("UPDATE lin_operational_config SET config_value='{\"enabled\":\"true\"}' WHERE config_key='legacy-hospital-funds' AND scope_type='GLOBAL' AND scope_id=0");assertTrue(service.balances().isEmpty());assertEquals(409,assertThrows(BusinessException.class,()->service.transactions(hospital,0,20,"all")).getCode());}
 @SuppressWarnings("unchecked") @Test void HistoricalLedgerDoesNotInventBalanceAfterAndCategoriesStayScoped(){
  store.insert("INSERT INTO lin_infohospital_money(infohospital_id,amount,remark,admin_id,createtime,updatetime) VALUES(?,2.50,'原加款',0,100,100)",hospital);store.insert("INSERT INTO lin_infohospital_money(infohospital_id,amount,remark,admin_id,createtime,updatetime) VALUES(?,-1.00,'原扣款',0,200,200)",hospital);
  var data=service.transactions(hospital,0,20,"expense");assertEquals(1,((Number)data.get("total")).intValue());var row=((List<Map<String,Object>>)data.get("items")).get(0);assertEquals("原扣款",row.get("memo"));assertFalse(row.containsKey("balanceAfter"));
  assertThrows(BusinessException.class,()->service.transactions(hospital,0,20,"invalid"));assertEquals(403,assertThrows(BusinessException.class,()->service.transactions(hospital+1,0,20,"all")).getCode());
 }
}
