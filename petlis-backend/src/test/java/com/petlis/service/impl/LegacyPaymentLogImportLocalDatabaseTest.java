package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyPaymentLogImportLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyPaymentLogService logs;LegacyPaymentLogImportService service;AccessService access;int actor;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_paylog_imp_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'流水导入测试','normal')",prefix);access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));logs=new LegacyPaymentLogService(store,access);service=new LegacyPaymentLogImportService(store,access,logs,"public-import-test-only-key");gate(true);}
 void gate(boolean enabled){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-payment-logs',?,'transaction-only import test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",true,"writeEnabled",true,"importEnabled",enabled)),actor);}
 String key(){return "payimp_"+UUID.randomUUID().toString().replace("-","");}
 String header(){return "BuyMail,BuyerId,TradeNoAli,TradeNoUser,TotalFee,TradeStatus,Subject,TradeBody,Exterface,CreateDt\n";}
 String row(String amount){return ",,"+prefix+","+prefix+","+amount+",CUSTOM,原文,,,2020-02-29 13:14:15\n";}
 LegacyPaymentLogImportService.Preview preview(String csv){return service.preview("a.csv",csv.getBytes(StandardCharsets.UTF_8),"AUTO","name");}
 LegacyPaymentLogImportService.ImportCommand command(LegacyPaymentLogImportService.Preview p,String key){return new LegacyPaymentLogImportService.ImportCommand(p.rows(),p.expiresAt(),p.previewToken(),key);}
 long seed(){var p=preview(header()+row("10"));return service.commit(command(p,key())).ids().get(0);}
 @Test void newRowsRetainDuplicatesRoundMoneyAndReplayWithoutWritingTwice(){var p=preview(header()+row("-1.23456789")+row("2.3456"));assertEquals(2,p.created());assertEquals(0,p.updated());assertFalse(p.warnings().isEmpty());assertEquals("-1.23",p.rows().get(0).fields().get("TotalFee"));var c=command(p,key());var r=service.commit(c);assertEquals(r,service.commit(c));assertEquals(2,r.ids().size());assertEquals(2,((Number)logs.list(prefix,0,20).get("total")).intValue());assertEquals("-1.23",logs.detail(r.ids().get(0)).get("TotalFee"));}
 @Test void partialPrimaryKeyUpdatesAreSequentialAndPreserveAbsentColumns(){long id=seed();var p=preview("PayLogId,TotalFee,Subject\n"+id+",20.005,第一行\n"+id+",-3.3333,最后一行\n");assertEquals(2,p.updated());assertEquals(0,p.created());var r=service.commit(command(p,key()));assertEquals(List.of(id,id),r.ids());var row=logs.detail(id);assertEquals("-3.33",row.get("TotalFee"));assertEquals("最后一行",row.get("Subject"));assertEquals(prefix,row.get("TradeNoUser"));assertEquals("2020-02-29 13:14:15",row.get("CreateDt"));}
 @Test void oldCommentHeadersIncludingEmptyDateCaptionAndUnknownColumnsAreMapped(){var p=service.preview("old.csv",("卖家账户,卖家支付宝ID,报告编号,样本编号,交易金额,交易状态,标题,内容,扩展,,忽略列\n,,"+prefix+","+prefix+",1,自定义,内容,,,2020-02-29 13:14:15,ignored\n").getBytes(StandardCharsets.UTF_8),"AUTO","comment");assertEquals(1,p.created());assertTrue(p.warnings().stream().anyMatch(w->w.contains("忽略")));assertEquals("2020-02-29 13:14:15",p.rows().get(0).fields().get("CreateDt"));service.commit(command(p,key()));}
 @Test void invalidRowsAndStalePreviewCannotCreateOrOverwrite(){long id=seed();var p=preview(header()+row("2"));logs.save(id,new LegacyPaymentLogService.Command(new LinkedHashMap<>(p.rows().get(0).fields()),key(),logs.detail(id).get("editToken").toString()));var stale=preview("PayLogId,TotalFee\n"+id+",3\n");var fields=new LinkedHashMap<>(stale.rows().get(0).fields());fields.put("TotalFee","4");logs.save(id,new LegacyPaymentLogService.Command(fields,key(),logs.detail(id).get("editToken").toString()));assertEquals(409,assertThrows(BusinessException.class,()->service.commit(command(stale,key()))).getCode());assertEquals("4.00",logs.detail(id).get("TotalFee"));assertEquals(400,assertThrows(BusinessException.class,()->preview(header()+row("1")+row("bad"))).getCode());assertEquals(1,((Number)logs.list(prefix,0,20).get("total")).intValue());}
 @Test void signedPreviewActorAndContentCannotBeChangedAndImportGateIsIndependent(){var p=preview(header()+row("1"));assertEquals(409,assertThrows(BusinessException.class,()->service.commit(new LegacyPaymentLogImportService.ImportCommand(p.rows(),p.expiresAt()+1,p.previewToken(),key()))).getCode());gate(false);assertEquals(409,assertThrows(BusinessException.class,()->preview(header()+row("1"))).getCode());assertEquals(409,assertThrows(BusinessException.class,()->service.commit(command(p,key()))).getCode());assertEquals(true,logs.capabilities().get("paymentLogWrites"));}
 @Test void actorBindingContentAndReusedBatchKeyAreChecked(){
  var p=preview(header()+row("1"));var fields=new LinkedHashMap<>(p.rows().get(0).fields());fields.put("TotalFee","2.00");var changed=List.of(new LegacyPaymentLogImportService.PreparedRow(2,null,fields,null));
  assertEquals(409,assertThrows(BusinessException.class,()->service.commit(new LegacyPaymentLogImportService.ImportCommand(changed,p.expiresAt(),p.previewToken(),key()))).getCode());
  when(access.current()).thenReturn(new CurrentActor(actor+1,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  assertEquals(409,assertThrows(BusinessException.class,()->service.commit(command(p,key()))).getCode());
  when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  String k=key();service.commit(command(p,k));var other=preview(header()+row("2"));assertEquals(409,assertThrows(BusinessException.class,()->service.commit(command(other,k))).getCode());
  assertTrue(new String(service.template(),StandardCharsets.UTF_8).startsWith("\ufeffBuyMail,BuyerId,"));
 }
}
