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
class LegacyPaymentLogLocalDatabaseTest {
 @Autowired FeatureStore store; LegacyPaymentLogService service; AccessService access; int actor; String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_paylog_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'支付流水测试','normal')",prefix);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyPaymentLogService(store,access);gate(true,true,true);}
 void gate(boolean enabled,boolean write,boolean delete){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-payment-logs',?,'transaction-only test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",enabled,"writeEnabled",write,"deleteEnabled",delete)),actor);}
 String key(){return "paylog_"+UUID.randomUUID().toString().replace("-","");}
 Map<String,String> fields(String money){var f=new LinkedHashMap<String,String>();for(String k:List.of("BuyMail","BuyerId","TradeNoAli","TradeNoUser","TradeStatus","Subject","TradeBody","Exterface"))f.put(k,"");f.put("TradeNoAli",prefix);f.put("TradeNoUser",prefix);f.put("TradeStatus","UNKNOWN_LEGACY_STATE");f.put("TotalFee",money);f.put("CreateDt","2020-02-29 13:14:15");return f;}
 LegacyPaymentLogService.Command command(Map<String,String> f,String version){return new LegacyPaymentLogService.Command(f,key(),version);}
 @Test void retainsEmptyCallbackFieldsNegativeMoneyDateAndDuplicateTransactionIds(){
  var c=command(fields("-12.3456"),null);long id=service.save(null,c);assertEquals(id,service.save(null,c));var row=service.detail(id);assertEquals("-12.35",row.get("TotalFee"));assertEquals("",row.get("BuyMail"));assertEquals("2020-02-29 13:14:15",row.get("CreateDt"));assertEquals("UNKNOWN_LEGACY_STATE",row.get("TradeStatus"));
  assertNotEquals(id,service.save(null,command(fields("12.3456"),null)));assertEquals(2,((Number)service.list(prefix,0,20).get("total")).intValue());
 }
 @Test void editDeleteReplayAndStaleVersions(){
  var f=fields("10");long id=service.save(null,command(f,null));String old=service.detail(id).get("editToken").toString();f.put("TotalFee","20.005");var edit=command(f,old);assertEquals(id,service.save(id,edit));assertEquals(id,service.save(id,edit));assertEquals("20.01",service.detail(id).get("TotalFee"));
  assertEquals(409,assertThrows(BusinessException.class,()->service.save(id,command(fields("1"),old))).getCode());
  var d=new LegacyPaymentLogService.DeleteCommand(key(),service.detail(id).get("editToken").toString());assertEquals(id,service.delete(id,d));assertEquals(id,service.delete(id,d));assertEquals(404,assertThrows(BusinessException.class,()->service.detail(id)).getCode());
 }
 @Test void replayComparesJsonStructurallyAndRejectsChangedCommands(){
  var f=fields("1");String key=key();long id=service.save(null,new LegacyPaymentLogService.Command(f,key,null));var reversed=new TreeMap<String,String>(Comparator.reverseOrder());reversed.putAll(f);assertEquals(id,service.save(null,new LegacyPaymentLogService.Command(reversed,key,null)));reversed.put("TotalFee","2");assertEquals(409,assertThrows(BusinessException.class,()->service.save(null,new LegacyPaymentLogService.Command(reversed,key,null))).getCode());
 }
 @Test void rejectsInvalidDatesOverflowUnknownColumnsAndGates(){
  for(String value:List.of("NaN","1e2","10000000000000000","9999999999999999.9999")){assertEquals(400,assertThrows(BusinessException.class,()->service.save(null,command(fields(value),null))).getCode());}
  var f=fields("1");f.put("CreateDt","2023-02-29 13:14:15");assertEquals(400,assertThrows(BusinessException.class,()->service.save(null,command(f,null))).getCode());f.put("CreateDt","2020-02-29 13:14:15");f.put("extra","sql");assertEquals(400,assertThrows(BusinessException.class,()->service.save(null,command(f,null))).getCode());
  long id=service.save(null,command(fields("1"),null));gate(true,true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.delete(id,new LegacyPaymentLogService.DeleteCommand(key(),service.detail(id).get("editToken").toString()))).getCode());gate(true,false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.save(null,command(fields("1"),null))).getCode());gate(false,true,true);assertEquals(409,assertThrows(BusinessException.class,()->service.list("",0,20)).getCode());doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.capabilities()).getCode());
 }
 @Test void doesNotChangeHospitalFundsOrdersOrPaymentRecords(){
  var tables=List.of("lin_lb_infohospital","lin_lb_listdiagnosisproject","lin_lb_diagnosisdetail","lin_lb_payordermain");var counts=new LinkedHashMap<String,Long>();for(String t:tables)counts.put(t,store.jdbc().queryForObject("SELECT COUNT(*) FROM "+t,Long.class));long id=service.save(null,command(fields("-100"),null));var f=fields("200");service.save(id,command(f,service.detail(id).get("editToken").toString()));for(String t:tables)assertEquals(counts.get(t),store.jdbc().queryForObject("SELECT COUNT(*) FROM "+t,Long.class));
 }
}
