package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyFinanceLocalDatabaseTest {
 @Autowired FeatureStore store;AccessService access;LegacyFinanceService service;LegacyOrderDetailService details;int actor,test;long hospital,order;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_finance_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'财务测试','normal')",prefix);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyFinanceService(store,access);details=new LegacyOrderDetailService(store,access,new LegacyReportLinks("https://petlis.invalid/","public-finance-test-key"));hospital=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,0,0)",prefix,prefix);order=LegacyDetailFixtures.order(store,prefix,actor);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId=?,SampleStatus=2 WHERE ListDiagnosisProjectId=?",hospital,order);test=LegacyDetailFixtures.test(store,prefix);gate(true,true);store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-order-details','{\"enabled\":true,\"writeEnabled\":true}','transaction-only finance fixture',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",actor);}
 void gate(boolean enabled,boolean write){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-finance',?,'transaction-only finance test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",enabled,"writeEnabled",write)),actor);}
 String key(){return "finance_"+UUID.randomUUID().toString().replace("-","");}
 long detail(String price,int exempt){return details.create(order,new LegacyOrderDetailService.CreateCommand(test,new BigDecimal(price),"10",new BigDecimal(price),prefix,0,"title",0,exempt,0,0,"<p>mail original</p>","<p>report original</p>",key()));}
 LegacyFinanceService.Target target(long id){return new LegacyFinanceService.Target(id,service.detail(id).get("editToken").toString());}
 @Test void pageTotalsUseExactMoneyAndExcludeExemptWithoutBecomingGlobalTotals(){
  detail("90071992547409.91",0);detail("0.09",0);detail("50.00",1);var all=service.list(hospital,prefix,0,50);assertEquals(3,((Number)all.get("total")).intValue());assertEquals("90071992547410.00",((Map<?,?>)all.get("pageSummary")).get("TestPrice"));
  @SuppressWarnings("unchecked") var rows=(List<Map<String,Object>>)all.get("items");assertEquals("免除",rows.get(0).get("PayStatus"));assertEquals("90071992547409.91",rows.get(2).get("TestPrice"));assertEquals("0.00",((Map<?,?>)service.list(hospital,prefix,0,1).get("pageSummary")).get("TestPrice"));
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId=0 WHERE ListDiagnosisProjectId=?",order);assertEquals(0,((Number)service.list(null,prefix,0,50).get("total")).intValue());
 }
 @Test void originalBulkStatusesAndInvoiceFlagsDoNotRunModelCallbacksOrMoveFunds(){
  long id=detail("125.50",0);var parent=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order);var body=store.one("SELECT * FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id=?",id);Object balance=store.one("SELECT amount FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital).get("amount");
  for(int state:List.of(1,2,4,0)){var command=new LegacyFinanceService.BatchCommand(List.of(target(id)),"PayStatus2",state,key());assertEquals(1,service.batch(command));assertEquals(1,service.batch(command));assertEquals(state,((Number)service.detail(id).get("PayStatus2")).intValue());}
  service.batch(new LegacyFinanceService.BatchCommand(List.of(target(id)),"is_invoiced",1,key()));assertEquals(1,((Number)service.detail(id).get("is_invoiced")).intValue());assertEquals(parent,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order));assertEquals(body,store.one("SELECT * FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id=?",id));assertEquals(balance,store.one("SELECT amount FROM lin_lb_infohospital WHERE InfoHospitalId=?",hospital).get("amount"));
 }
 @Test void staleBatchIsAllOrNothingAndChangedRequestsCannotReplay(){
  long first=detail("1.00",0),second=detail("2.00",0);var a=target(first);var b=target(second);service.batch(new LegacyFinanceService.BatchCommand(List.of(b),"PayStatus2",2,key()));
  assertEquals(409,assertThrows(BusinessException.class,()->service.batch(new LegacyFinanceService.BatchCommand(List.of(a,b),"PayStatus2",1,key()))).getCode());assertEquals(0,((Number)service.detail(first).get("PayStatus2")).intValue());
  String request=key();service.batch(new LegacyFinanceService.BatchCommand(List.of(target(first)),"PayStatus2",1,request));assertEquals(409,assertThrows(BusinessException.class,()->service.batch(new LegacyFinanceService.BatchCommand(List.of(target(first)),"PayStatus2",0,request))).getCode());
 }
 @Test void fieldWhitelistPermissionsAndFlagsAreEnforced(){
  long id=detail("1.00",0);assertEquals(400,assertThrows(BusinessException.class,()->service.batch(new LegacyFinanceService.BatchCommand(List.of(target(id)),"PayStatus2",3,key()))).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.batch(new LegacyFinanceService.BatchCommand(List.of(target(id)),"SampleStatus",3,key()))).getCode());
  gate(true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.batch(new LegacyFinanceService.BatchCommand(List.of(target(id)),"is_invoiced",1,key()))).getCode());gate(false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.list(hospital,"",0,50)).getCode());doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,service::capabilities).getCode());
 }
}
