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
class LegacyHospitalContactLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyHospitalContactService service;AccessService access;int actor;long hospital,other;
 @BeforeEach void setup(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));String prefix="it_contact_"+UUID.randomUUID().toString().substring(0,8);
  actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'联系人测试','normal')",prefix);
  hospital=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,0,0)",prefix,prefix);
  other=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,0,0)",prefix+"other",prefix);
  access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  service=new LegacyHospitalContactService(store,access);gate(true,true);
 }
 void gate(boolean enabled,boolean write){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-hospital-contacts',?,'transaction-only contact test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",enabled,"writeEnabled",write)),actor);}
 LegacyHospitalContactService.Command command(String token,String key){return new LegacyHospitalContactService.Command(" 姓名 ","电话","a@example.test","传真","备注",1,key,token);}
 String key(){return "contact_"+UUID.randomUUID().toString().replace("-","");}
 @Test void saveReplayAndUpdateKeepTimestampAndCredentialsPrivate(){
  var c=command(null,key());long id=service.save(hospital,null,c);assertEquals(id,service.save(hospital,null,c));var row=service.detail(hospital,id);assertEquals(" 姓名 ",row.get("Name"));assertEquals(1,((Number)row.get("isFinanceContact")).intValue());assertFalse(row.containsKey("LoginPwd"));assertFalse(row.containsKey("LoginName"));
  store.jdbc().update("UPDATE lin_lb_infohospitalcontact SET LoginName='historical-login',LoginPwd='historical-secret' WHERE InfoHospitalContactId=?",id);
  row=service.detail(hospital,id);var change=command(row.get("editToken").toString(),key());service.save(hospital,id,change);assertEquals(id,service.save(hospital,id,change));assertEquals(row.get("CreateDt"),service.detail(hospital,id).get("CreateDt"));assertEquals("historical-secret",store.one("SELECT LoginPwd FROM lin_lb_infohospitalcontact WHERE InfoHospitalContactId=?",id).get("LoginPwd"));
  assertEquals(1,((Number)service.list(hospital,0,20).get("total")).intValue());assertEquals(0,((Number)service.list(other,0,20).get("total")).intValue());
 }
 @Test void deleteRequiresIndependentFlagAndSupportsReplay(){
  long id=service.save(hospital,null,command(null,key()));var delete=new LegacyHospitalContactService.DeleteCommand(key(),service.detail(hospital,id).get("editToken").toString());
  assertEquals(409,assertThrows(BusinessException.class,()->service.delete(hospital,id,delete)).getCode());
  store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-hospital-contacts' AND scope_type='GLOBAL' AND scope_id=0",store.encode(Map.of("enabled",true,"writeEnabled",true,"deleteEnabled",true)));
  assertEquals(404,assertThrows(BusinessException.class,()->service.delete(other,id,delete)).getCode());
  assertEquals(id,service.delete(hospital,id,delete));assertEquals(id,service.delete(hospital,id,delete));assertEquals(0,((Number)service.list(hospital,0,20).get("total")).intValue());
 }
 @Test void recipientUsesOriginalTrimAndDoesNotFilterOutNonFinanceContacts(){
  long id=service.save(hospital,null,new LegacyHospitalContactService.Command("报告联系人","","  report@example.test  ","","",0,key(),null));
  assertEquals("report@example.test",service.recipient(hospital,id).get("mail"));
  assertEquals(0,((Number)service.recipient(hospital,id).get("financeContact")).intValue());
  assertEquals(404,assertThrows(BusinessException.class,()->service.recipient(other,id)).getCode());
 }
 @Test void staleEditAndCrossHospitalWriteAreRejected(){long id=service.save(hospital,null,command(null,key()));String token=service.detail(hospital,id).get("editToken").toString();store.jdbc().update("UPDATE lin_lb_infohospitalcontact SET Name='external update' WHERE InfoHospitalContactId=?",id);assertEquals(409,assertThrows(BusinessException.class,()->service.save(hospital,id,command(token,key()))).getCode());assertEquals(404,assertThrows(BusinessException.class,()->service.save(other,id,command(token,key()))).getCode());}
 @Test void strictFlagsAndAdminAuthorizationProtectBothReadAndWrite(){gate(true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.save(hospital,null,command(null,key()))).getCode());gate(false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.list(hospital,0,20)).getCode());doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.capabilities()).getCode());}
}
