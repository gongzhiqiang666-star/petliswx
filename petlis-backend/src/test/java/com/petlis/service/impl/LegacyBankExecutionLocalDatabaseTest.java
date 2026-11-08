package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.transaction.annotation.Transactional;
import java.util.*;import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @org.springframework.context.annotation.Import(LegacyBankJdbcTestConfiguration.class) @Transactional("legacyBankTestTransactions") @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyBankExecutionLocalDatabaseTest {
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @Autowired FeatureStore store;@Autowired ObjectMapper json;LegacyBankPaymentQuoteLocalDatabaseTest f;LegacyBankPaymentExecutionService execution;String key;
 @BeforeEach void setup(){f=new LegacyBankPaymentQuoteLocalDatabaseTest();f.store=store;f.json=json;f.setup();f.detail(246,"12.50",0,0);key=f.prefix;
  store.jdbc().update("UPDATE lin_operational_config SET config_value='{\"enabled\":true,\"prepareEnabled\":true,\"sendEnabled\":true,\"queryEnabled\":true}' WHERE config_key='legacy-bank-payments'");
  new LegacyBankPaymentPreparationService(store,f.access,f.service).prepare(new LegacyBankPaymentPreparationService.Command("ORDER",f.hospital,null,f.prefix,"12.50",key),"LOCALTEST");
  execution=new LegacyBankPaymentExecutionService(store,f.access,json);
 }
 @Test void claimedRequestCannotBeResentAndUnknownResultOnlyAllowsQuery(){var send=execution.claim(key,"LOCALTEST",false);assertEquals("SENDING",state());assertEquals("1250",send.parameters().get("trxTtlAmt"));assertEquals(f.hospital,((Number)send.parameters().get("userNo")).longValue());assertThrows(BusinessException.class,()->execution.claim(key,"LOCALTEST",false));execution.unknown(key,send.token());assertEquals("UNKNOWN",state());assertThrows(BusinessException.class,()->execution.claim(key,"LOCALTEST",false));var query=execution.claim(key,"LOCALTEST",true);assertEquals(send.orderId(),query.parameters().get("oriMerOrdrNo"));assertEquals("QUERYING",state());}
 @Test void querySettlesOriginalFieldsOnceAndDoesNotRunClinicalSaveHooks()throws Exception{
  var parent=store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.order);var detail=store.one("SELECT * FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",f.order);var body=store.one("SELECT * FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id=?",detail.get("DiagnosisDetailId"));
  var send=execution.claim(key,"LOCALTEST",false);execution.unknown(key,send.token());var query=execution.claim(key,"LOCALTEST",true);
  var response=json.readTree("{\"retCode\":\"00000\",\"tradeStatus\":\"00\",\"oriMerOrdrNo\":\""+send.orderId()+"\",\"trxTtlAmt\":\"1250\",\"chnlMerOrdrNo\":\"test-channel\",\"subOpenId\":\"test-buyer\"}");
  execution.finish(key,query.token(),true,response);assertEquals("SUCCEEDED",state());execution.finish(key,query.token(),true,response);
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser=?",Integer.class,send.orderId()));
  var main=store.one("SELECT * FROM lin_lb_payordermain WHERE OrderId=?",send.orderId());assertEquals("test-channel",main.get("AliTradeId"));assertEquals(0,((Number)main.get("IsSuccess")).intValue());
  var expectedParent=new LinkedHashMap<>(parent);expectedParent.put("IsPayMoney",3);assertEquals(expectedParent,store.one("SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",f.order));
  var expectedDetail=new LinkedHashMap<>(detail);expectedDetail.put("PayStatus2",3);assertEquals(expectedDetail,store.one("SELECT * FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId=?",detail.get("DiagnosisDetailId")));
  assertEquals(body,store.one("SELECT * FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id=?",detail.get("DiagnosisDetailId")));
  var log=store.one("SELECT * FROM lin_lb_paylog WHERE TradeNoUser=?",send.orderId());assertEquals("test-buyer",log.get("BuyerId"));assertEquals("TRADE_SUCCESS",log.get("TradeStatus"));store.jdbc().update("UPDATE lin_operational_config SET config_value=JSON_SET(config_value,'$.notifyEnabled',true) WHERE config_key='legacy-bank-payments'");var notice=response.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)notice).put("buyerLgnId","different-notice-buyer");assertTrue(execution.notification(notice,"LOCALTEST"));assertEquals("test-buyer",store.one("SELECT BuyerId FROM lin_lb_paylog WHERE TradeNoUser=?",send.orderId()).get("BuyerId"));
 }
 @Test void mismatchedAmountOrOrderCannotSettleAndLateSendCannotOverwriteQuery()throws Exception{
  var send=execution.claim(key,"LOCALTEST",false);var query=execution.claim(key,"LOCALTEST",true);
  execution.finish(key,send.token(),false,json.readTree("{\"retCode\":\"00000\",\"chnlUrl\":\"https://cashier.invalid/\"}"));assertEquals("QUERYING",state());
  execution.finish(key,query.token(),true,json.readTree("{\"retCode\":\"00000\",\"tradeStatus\":\"00\",\"oriMerOrdrNo\":\""+send.orderId()+"\",\"trxTtlAmt\":\"1\",\"chnlMerOrdrNo\":\"test-channel\"}"));
  assertEquals("UNKNOWN",state());assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser=?",Integer.class,send.orderId()));assertEquals(0,((Number)store.one("SELECT PayStatus2 FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",f.order).get("PayStatus2")).intValue());
 }
 String state(){return store.one("SELECT status FROM lin_order_idempotency WHERE user_id=? AND operation='LEGACY_BANK_PREPARE' AND idempotency_key=?",f.actor,key).get("status").toString();}
 @Test void cashierAcceptanceDoesNotMeanPaidAndCannotSendAgain()throws Exception{var send=execution.claim(key,"LOCALTEST",false);execution.finish(key,send.token(),false,json.readTree("{\"retCode\":\"00000\",\"chnlUrl\":\"https://cashier.example.test/pay\"}"));assertEquals("CASHIER_READY",state());assertNull(execution.claim(key,"LOCALTEST",false));assertEquals(0,((Number)store.one("SELECT PayStatus2 FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",f.order).get("PayStatus2")).intValue());var query=execution.claim(key,"LOCALTEST",true);assertNotNull(query);assertThrows(BusinessException.class,()->execution.claim(key,"LOCALTEST",true));}
 @Test void ledgerFailureRollsBackReceiptAndClinicalFlagsAndCanRecover()throws Exception{
  var send=execution.claim(key,"LOCALTEST",false);execution.unknown(key,send.token());var query=execution.claim(key,"LOCALTEST",true);
  var response=json.readTree("{\"retCode\":\"00000\",\"tradeStatus\":\"00\",\"oriMerOrdrNo\":\""+send.orderId()+"\",\"trxTtlAmt\":\"1250\",\"chnlMerOrdrNo\":\"test-channel\"}");
  var faulty=new FeatureStore(store.jdbc(),json){@Override public long insert(String sql,Object...args){if(sql.startsWith("INSERT INTO lin_lb_paylog"))throw new BusinessException(409,"synthetic ledger failure");return super.insert(sql,args);}};
  var broken=new LegacyBankPaymentExecutionService(faulty,f.access,json);var tx=new org.springframework.transaction.support.TransactionTemplate(transactions);tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
  var manager=(org.springframework.transaction.support.AbstractPlatformTransactionManager)transactions;boolean nestedBefore=manager.isNestedTransactionAllowed();manager.setNestedTransactionAllowed(true);try{
  assertEquals("synthetic ledger failure",assertThrows(BusinessException.class,()->tx.execute(status->{broken.finish(key,query.token(),true,response);return null;})).getMessage());
  }finally{manager.setNestedTransactionAllowed(nestedBefore);}
  assertEquals("QUERYING",state());assertEquals("",store.one("SELECT AliTradeId FROM lin_lb_payordermain WHERE OrderId=?",send.orderId()).get("AliTradeId"));assertEquals(0,((Number)store.one("SELECT PayStatus2 FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId=?",f.order).get("PayStatus2")).intValue());
  execution.finish(key,query.token(),true,response);assertEquals("SUCCEEDED",state());assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser=?",Integer.class,send.orderId()));
 }
 @Test void notificationUsesBuyerLgnIdWithoutLoginAndDuplicateDoesNotAddLedger()throws Exception{
  store.jdbc().update("UPDATE lin_operational_config SET config_value=JSON_SET(config_value,'$.notifyEnabled',true) WHERE config_key='legacy-bank-payments'");
  var send=execution.claim(key,"LOCALTEST",false);var payload=json.readTree("{\"tradeStatus\":\"00\",\"oriMerOrdrNo\":\""+send.orderId()+"\",\"trxTtlAmt\":\"1250\",\"chnlMerOrdrNo\":\"notice-channel\",\"buyerLgnId\":\"notice-buyer\",\"subOpenId\":\"wrong-query-buyer\"}");
  org.mockito.Mockito.clearInvocations(f.access);assertTrue(execution.notification(payload,"LOCALTEST"));assertTrue(execution.notification(payload,"LOCALTEST"));
  org.mockito.Mockito.verifyNoInteractions(f.access);assertEquals("SUCCEEDED",state());assertEquals("notice-buyer",store.one("SELECT BuyerId FROM lin_lb_paylog WHERE TradeNoUser=?",send.orderId()).get("BuyerId"));assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser=?",Integer.class,send.orderId()));
  execution.finish(key,send.token(),false,json.readTree("{\"retCode\":\"00000\",\"chnlUrl\":\"https://cashier.example.test/pay\"}"));assertEquals("SUCCEEDED",state());
 }
 @Test void notificationCannotSettleUnsentOrderOrBypassIndependentGate()throws Exception{
  var order=store.one("SELECT OrderId FROM lin_lb_payordermain WHERE Remark=?","订单:"+key).get("OrderId");var payload=json.readTree("{\"tradeStatus\":\"00\",\"oriMerOrdrNo\":\""+order+"\",\"trxTtlAmt\":\"1250\",\"chnlMerOrdrNo\":\"notice-channel\"}");
  assertThrows(BusinessException.class,()->execution.notification(payload,"LOCALTEST"));store.jdbc().update("UPDATE lin_operational_config SET config_value=JSON_SET(config_value,'$.notifyEnabled',true) WHERE config_key='legacy-bank-payments'");assertFalse(execution.notification(payload,"LOCALTEST"));assertEquals("READY",state());assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser=?",Integer.class,order));
 }
 @Test void notificationRejectsWrongAmountAndMerchantWithoutDowngradingPaidReceipt()throws Exception{
  store.jdbc().update("UPDATE lin_operational_config SET config_value=JSON_SET(config_value,'$.notifyEnabled',true) WHERE config_key='legacy-bank-payments'");var send=execution.claim(key,"LOCALTEST",false);
  var payload=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree("{\"tradeStatus\":\"00\",\"oriMerOrdrNo\":\""+send.orderId()+"\",\"trxTtlAmt\":\"1\",\"chnlMerOrdrNo\":\"notice-channel\"}");
  assertFalse(execution.notification(payload,"OTHER"));assertEquals("SENDING",state());assertFalse(execution.notification(payload,"LOCALTEST"));assertEquals("UNKNOWN",state());assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser=?",Integer.class,send.orderId()));
  payload.put("trxTtlAmt","1250");assertTrue(execution.notification(payload,"LOCALTEST"));payload.put("trxTtlAmt","1");assertFalse(execution.notification(payload,"LOCALTEST"));assertEquals("SUCCEEDED",state());assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser=?",Integer.class,send.orderId()));
 }
}
