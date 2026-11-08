package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacySampleNoticeLocalDatabaseTest {
 @Autowired FeatureStore store;@Autowired PlatformTransactionManager manager;
 AccessService access;LegacySampleNoticeService service;LegacyMailTransport transport;LegacyMailTransportTest.Sink sink;
 String prefix;int actor;long order,hospital,contact,template,profile,binding;String oldConfig;
 String key(){return "notice_"+UUID.randomUUID().toString().replace("-","");}
 @BeforeEach void setup(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_notice_"+UUID.randomUUID().toString().substring(0,8);
  actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'收样通知测试','normal')",prefix);
  hospital=store.insert("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES(?,?,0,20)",prefix,prefix);
  order=LegacyDetailFixtures.order(store,prefix,actor);store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId=?,AnimalName=?,SampleStatus=2,IsSendNoticeMail=0 WHERE ListDiagnosisProjectId=?",hospital,prefix,order);
  contact=store.insert("INSERT INTO lin_lb_infohospitalcontact(InfoHospitalId,isFinanceContact,Name,Tel,Mail,Fax,ContactRemark,CreateDt) VALUES(?,0,?,'','doctor@example.test','','',NOW())",hospital,prefix);
  template=store.insert("INSERT INTO lin_lb_dictmailreportnotice(tplname,tpltext,is_active,createtime) VALUES(?,?,0,123)",prefix,"<p>{"+prefix+"} {当前时间}</p>");
  binding=store.insert("INSERT INTO lin_lb_dictbindkey(BindTable,ColumnName,DescName) VALUES('ListDiagnosisProject','AnimalName',?)",prefix);
  profile=store.jdbc().queryForObject("SELECT COALESCE(MAX(DictMailServerId),0)+1 FROM lin_lb_dictmailserver",Long.class);store.jdbc().update("INSERT INTO lin_lb_dictmailserver VALUES(?,'unresolvable.invalid',587,'sender@laboklin.cn','synthetic-only','copy@example.test')",profile);
  var configs=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-sample-notice' AND scope_type='GLOBAL' AND scope_id=0");oldConfig=configs.isEmpty()?null:configs.get(0).get("config_value").toString();gate(true,true,true);
  access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  sink=new LegacyMailTransportTest.Sink();transport=spy(new LegacyMailTransport("TEST_LOCAL",sink.port(),false));
  service=new LegacySampleNoticeService(store,access,new LegacyOrderDetailService(store,access,new LegacyReportLinks("https://petlis.invalid/","synthetic-public-key")),transport,manager,(int)profile,"","synthetic-private-test-signing-key");
 }
 void gate(boolean read,boolean write,boolean send){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-sample-notice',?,'synthetic notice fixture',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)",store.encode(Map.of("enabled",read,"writeEnabled",write,"sendEnabled",send)),actor);}
 @AfterEach void cleanup(){if(sink!=null)sink.close();if(actor==0)return;store.jdbc().update("DELETE a FROM lin_mail_attempt a JOIN lin_mail_message m ON m.id=a.message_id WHERE m.requested_by=? AND m.business_type='SAMPLE_NOTICE'",actor);store.jdbc().update("DELETE FROM lin_mail_message WHERE requested_by=? AND business_type='SAMPLE_NOTICE'",actor);store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_SAMPLE_NOTICE'",actor);if(oldConfig==null)store.jdbc().update("DELETE FROM lin_operational_config WHERE config_key='legacy-sample-notice' AND description='synthetic notice fixture'");else store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-sample-notice' AND scope_type='GLOBAL' AND scope_id=0",oldConfig);store.jdbc().update("DELETE FROM lin_lb_dictmailserver WHERE DictMailServerId=? AND ServerHost='unresolvable.invalid'",profile);store.jdbc().update("DELETE FROM lin_lb_dictbindkey WHERE id=? AND DescName=?",binding,prefix);store.jdbc().update("DELETE FROM lin_lb_dictmailreportnotice WHERE id=? AND tplname=?",template,prefix);store.jdbc().update("DELETE FROM lin_lb_infohospitalcontact WHERE InfoHospitalContactId=? AND InfoHospitalId=?",contact,hospital);store.jdbc().update("DELETE FROM lin_lb_notification WHERE order_id=?",order);store.jdbc().update("DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=? AND UserNO=?",order,prefix);store.jdbc().update("DELETE FROM lin_lb_infohospital WHERE InfoHospitalId=? AND HospitalName=?",hospital,prefix);store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",actor,prefix);}
 LegacySampleNoticeService.Command command(String key){return new LegacySampleNoticeService.Command(contact,"样本收到通知","<p>已收到合成标本 中文 α</p>",key,service.draft(order,template).get("editToken").toString());}
 @Test void draftUsesActualBindingsAndQueueIsIdempotentWithoutSending(){var draft=service.draft(order,template);assertTrue(draft.get("html").toString().contains(prefix));assertFalse(draft.get("html").toString().contains("{"+prefix+"}"));var command=command(key());long id=service.queue(order,command);assertEquals(id,service.queue(order,command));assertEquals("READY",service.message(id).get("status"));verify(transport,never()).send(any(),any(),any());assertEquals(0,((Number)store.one("SELECT IsSendNoticeMail FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order).get("IsSendNoticeMail")).intValue());assertEquals(409,assertThrows(BusinessException.class,()->service.queue(order,new LegacySampleNoticeService.Command(contact,"different",command.html(),command.requestKey(),command.expectedToken()))).getCode());assertEquals(404,assertThrows(BusinessException.class,()->service.queue(order,new LegacySampleNoticeService.Command(contact+999999,"title","body",key(),command.expectedToken()))).getCode());}
 @Test void localSmtpAcceptanceUpdatesOnlyNoticeFlagAndReplayDoesNotSendAgain()throws Exception{long id=service.queue(order,command(key()));assertEquals("SENT",service.dispatch(id).get("status"));assertEquals("样本收到通知",sink.received.get(8,TimeUnit.SECONDS).getSubject());assertEquals("SENT",service.dispatch(id).get("status"));verify(transport,times(1)).send(any(),any(),eq(LegacyMailRoutingRules.Kind.SAMPLE_NOTICE));var row=store.one("SELECT SampleStatus,IsSendNoticeMail,CAST(TestFee AS CHAR) AS TestFee FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order);assertEquals(2,((Number)row.get("SampleStatus")).intValue());assertEquals(1,((Number)row.get("IsSendNoticeMail")).intValue());assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_mail_attempt WHERE message_id=?",Integer.class,id));}
 @Test void uncertainDeliveryNeverMarksSentAndCannotAutomaticallyRetry(){long id=service.queue(order,command(key()));doThrow(new LegacyMailTransport.DeliveryUncertainException()).when(transport).send(any(),any(),any());assertEquals("UNKNOWN",service.dispatch(id).get("status"));assertEquals(409,assertThrows(BusinessException.class,()->service.dispatch(id)).getCode());verify(transport,times(1)).send(any(),any(),any());assertEquals(0,((Number)store.one("SELECT IsSendNoticeMail FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order).get("IsSendNoticeMail")).intValue());}
 @Test void acceptedMailRecoversFailedCallbackWithoutSendingAgain(){
  long id=service.queue(order,command(key()));
  doAnswer(invocation->{invocation.callRealMethod();store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId=0 WHERE ListDiagnosisProjectId=?",order);return null;}).when(transport).send(any(),any(),any());
  assertEquals("ACCEPTED",service.dispatch(id).get("status"));assertEquals("FAILED",service.message(id).get("callback_status"));
  assertEquals(0,((Number)store.one("SELECT IsSendNoticeMail FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=?",order).get("IsSendNoticeMail")).intValue());
  store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId=? WHERE ListDiagnosisProjectId=?",hospital,order);
  gate(true,true,false);assertEquals("SENT",service.dispatch(id).get("status"));verify(transport,times(1)).send(any(),any(),any());
 }
 @Test void anotherRequestCannotBypassPendingMailAndReadyCanBeCancelled(){
  long first=service.queue(order,command(key()));assertEquals(409,assertThrows(BusinessException.class,()->service.queue(order,command(key()))).getCode());
  assertEquals("CANCELLED",service.cancel(first).get("status"));assertEquals("CANCELLED",service.cancel(first).get("status"));
  long second=service.queue(order,command(key()));assertNotEquals(first,second);doThrow(new LegacyMailTransport.DeliveryUncertainException()).when(transport).send(any(),any(),any());service.dispatch(second);
  assertEquals(409,assertThrows(BusinessException.class,()->service.queue(order,command(key()))).getCode());assertEquals(409,assertThrows(BusinessException.class,()->service.cancel(second)).getCode());
 }
 @Test void closedFlagsAndChangedOrderRejectBeforeSmtp(){var c=command(key());store.jdbc().update("UPDATE lin_lb_listdiagnosisproject SET AnimalName=? WHERE ListDiagnosisProjectId=?",prefix+"changed",order);assertEquals(409,assertThrows(BusinessException.class,()->service.queue(order,c)).getCode());long id=service.queue(order,command(key()));assertEquals(409,assertThrows(BusinessException.class,()->new org.springframework.transaction.support.TransactionTemplate(manager).execute(tx->service.dispatch(id))).getCode());gate(true,true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.dispatch(id)).getCode());verify(transport,never()).send(any(),any(),any());gate(false,true,true);assertEquals(409,assertThrows(BusinessException.class,()->service.draft(order,template)).getCode());}
}
