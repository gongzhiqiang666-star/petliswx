package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.transaction.annotation.Transactional;
import java.util.*;import java.math.BigDecimal;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyBankPaymentQuoteLocalDatabaseTest {
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @Autowired FeatureStore store;AccessService access;LegacyBankPaymentQuoteService service;LegacyOrderDetailService details;int actor,test;long hospital,order;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_bank_quote_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'网银核价测试','normal')",prefix);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyBankPaymentQuoteService(store,access);details=new LegacyOrderDetailService(store,access,new LegacyReportLinks("https://petlis.invalid/","public-bank-quote-test"));hospital=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,1,0)",prefix,prefix);order=LegacyDetailFixtures.order(store,prefix,actor);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId=?,ReceiveSampleDt='1900-01-01 00:00:00' WHERE ListDiagnosisProjectId=?",hospital,order);test=LegacyDetailFixtures.test(store,prefix);gate(true);store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-order-details','{\"enabled\":true,\"writeEnabled\":true}','transaction-only quote fixture',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",actor);}
 void gate(boolean enabled){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-bank-payments',?,'transaction-only quote test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",enabled)),actor);}
 void detail(int type,String price,int paid,int exempt){details.create(order,new LegacyOrderDetailService.CreateCommand(type,new BigDecimal(price),"10",new BigDecimal(price),prefix,0,"",0,exempt,paid,0,"mail","report","bank_"+UUID.randomUUID().toString().replace("-","")));}
 @Test void orderQuoteIncludesUnreceived246OnlyAndNeverChangesPaymentState(){detail(246,"12.50",0,0);detail(test,"9.99",0,0);detail(246,"1.00",1,0);detail(246,"2.00",0,1);var before=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order);var q=service.quote("ORDER",null,null,prefix);assertEquals("12.50",q.get("amount"));assertEquals("12.50",q.get("displayAmount"));assertEquals("1250",q.get("amountFen"));assertEquals(1,((List<?>)q.get("items")).size());assertEquals(before,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order));store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET ReceiveSampleDt='2024-02-29 23:59:59' WHERE ListDiagnosisProjectId=?",order);assertEquals("22.49",service.quote("ORDER",null,null,prefix).get("amount"));}
 @Test void monthQuoteUsesHospitalAndExcludesFinalSecondExactlyAsPhp(){detail(test,"7.25",0,0);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET ReceiveSampleDt='2024-02-29 23:59:58' WHERE ListDiagnosisProjectId=?",order);assertEquals("7.25",service.quote("MONTH",hospital,"2024-02",null).get("amount"));store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET ReceiveSampleDt='2024-02-29 23:59:59' WHERE ListDiagnosisProjectId=?",order);assertEquals("0.00",service.quote("MONTH",hospital,"2024-02",null).get("amount"));}
 @Test void quoteRejectsInvalidInputsAndRequiresIndependentAdminGate(){assertEquals(400,assertThrows(BusinessException.class,()->service.quote("MONTH",hospital,"2024-13",null)).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.quote("ORDER",null,null,"")).getCode());gate(false);assertEquals(409,assertThrows(BusinessException.class,()->service.quote("ORDER",null,null,prefix)).getCode());doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,service::capabilities).getCode());}
 @Test void paymentPreparationPersistsOriginalAllocationsAndReplaysWithoutNewRows(){
  detail(246,"12.50",0,0);detail(test,"9.99",0,0);
  store.jdbc().update("UPDATE lin_operational_config SET config_value='{\"enabled\":true,\"prepareEnabled\":true}' WHERE config_key='legacy-bank-payments'");
  var payments=new LegacyBankPaymentPreparationService(store,access,service);
  var command=new LegacyBankPaymentPreparationService.Command("ORDER",hospital,null,prefix,"12.50",prefix);
  var parent=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order);
  var result=payments.prepare(command,"TESTMERCHANT");
  assertEquals(result,payments.prepare(command,"TESTMERCHANT"));
  var main=store.one("SELECT * FROM lin_lb_payordermain WHERE OrderId=?",result.orderId());
  assertEquals(0,((Number)main.get("HostId")).intValue());assertEquals("",main.get("AliTradeId"));assertEquals(0,((Number)main.get("IsSuccess")).intValue());assertEquals("",main.get("PayMonth"));
  assertEquals(0,new BigDecimal(main.get("CHKMoney").toString()).compareTo(new BigDecimal("12.50")));
  var allocations=store.jdbc().queryForList("SELECT * FROM lin_lb_payorderdetail WHERE MainOrderId=?",result.orderId());assertEquals(1,allocations.size());assertEquals(allocations.get(0).get("PayMoney"),allocations.get(0).get("ShowMoney"));
  assertEquals(prefix+"_"+allocations.get(0).get("DetailId"),main.get("Md5Id"));
  assertEquals(parent,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_payordermain WHERE OrderId=?",Integer.class,result.orderId()));
  assertEquals(409,assertThrows(BusinessException.class,()->payments.prepare(new LegacyBankPaymentPreparationService.Command("ORDER",hospital,null,prefix,"99.00",prefix),"TESTMERCHANT")).getCode());
 }
 @Test void preparationRequiresOwnGateAndRejectsStaleAmountBeforePersisting(){
  detail(246,"12.50",0,0);var payments=new LegacyBankPaymentPreparationService(store,access,service);
  var command=new LegacyBankPaymentPreparationService.Command("ORDER",hospital,null,prefix,"99.00",prefix);
  assertEquals(409,assertThrows(BusinessException.class,()->payments.prepare(command,"TESTMERCHANT")).getCode());
  store.jdbc().update("UPDATE lin_operational_config SET config_value='{\"enabled\":true,\"prepareEnabled\":true}' WHERE config_key='legacy-bank-payments'");
  assertEquals(409,assertThrows(BusinessException.class,()->payments.prepare(command,"TESTMERCHANT")).getCode());
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_payordermain WHERE Remark=?",Integer.class,"订单:"+prefix));
 }
 @Test void monthPreparationPreservesLegacyMonthIdentityAndSourceAmounts(){
  detail(test,"7.25",0,0);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET ReceiveSampleDt='2024-02-29 23:59:58' WHERE ListDiagnosisProjectId=?",order);
  store.jdbc().update("UPDATE lin_operational_config SET config_value='{\"enabled\":true,\"prepareEnabled\":true}' WHERE config_key='legacy-bank-payments'");
  var payments=new LegacyBankPaymentPreparationService(store,access,service);
  var result=payments.prepare(new LegacyBankPaymentPreparationService.Command("MONTH",hospital,"2024-02",null,"7.25",prefix),"TESTMERCHANT");
  var main=store.one("SELECT * FROM lin_lb_payordermain WHERE OrderId=?",result.orderId());
  assertEquals("2024-02-01 00:00:00",main.get("PayMonth"));assertEquals(hospital+"_2024-02-01至2024-02-29",main.get("Md5Id"));
  assertEquals("账单:2024-02-01至2024-02-29_"+prefix,main.get("Remark"));assertEquals("725",result.amountFen());
  assertEquals("READY",store.one("SELECT status FROM lin_order_idempotency WHERE user_id=? AND operation='LEGACY_BANK_PREPARE' AND idempotency_key=?",actor,prefix).get("status"));
 }
 @Test void persistenceFailureRollsBackMainAllocationsAndRequestTogether(){
  detail(246,"12.50",0,0);store.jdbc().update("UPDATE lin_operational_config SET config_value='{\"enabled\":true,\"prepareEnabled\":true}' WHERE config_key='legacy-bank-payments'");
  var faulty=new FeatureStore(store.jdbc(),json){
   @Override public long insert(String sql,Object... args){if(sql.startsWith("INSERT INTO lin_audit_record"))throw new BusinessException(409,"synthetic audit failure");return super.insert(sql,args);}
  };
  var payment=new LegacyBankPaymentPreparationService(faulty,access,service);
  var transaction=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(store.jdbc().getDataSource()));transaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
  var ex=assertThrows(BusinessException.class,()->transaction.execute(status->payment.prepare(new LegacyBankPaymentPreparationService.Command("ORDER",hospital,null,prefix,"12.50",prefix),"TESTMERCHANT")));
  assertEquals("synthetic audit failure",ex.getMessage());
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_payordermain WHERE Remark=?",Integer.class,"订单:"+prefix));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_payorderdetail WHERE UserNo=?",Integer.class,prefix));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_order_idempotency WHERE user_id=? AND operation='LEGACY_BANK_PREPARE' AND idempotency_key=?",Integer.class,actor,prefix));
 }
}
