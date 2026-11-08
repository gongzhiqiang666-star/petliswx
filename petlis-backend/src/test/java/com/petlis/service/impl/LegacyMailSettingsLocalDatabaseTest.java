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
class LegacyMailSettingsLocalDatabaseTest {
 @Autowired FeatureStore store;
 LegacyMailSettingsService service; AccessService access; int actor;
 String key(){return "mail_"+UUID.randomUUID().toString().replace("-","");}
 void gate(boolean read,boolean write,boolean delete){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-mail-settings',?,'transaction-only SMTP maintenance test',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",read,"writeEnabled",write,"deleteEnabled",delete)),actor);}
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'邮件配置测试','normal')",key().substring(0,24));access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyMailSettingsService(store,access,"synthetic-test-signing-key-with-no-production-use");gate(true,true,true);}
 LegacyMailSettingsService.Command command(String password,String nonce,String version){return new LegacyMailSettingsService.Command("smtp.example.test",587,"synthetic@example.test",password,"copy@example.test",nonce,version);}
 @Test void createEditReplayAndDeleteKeepSecretsOutOfResponsesAndAudit(){
  String secret="synthetic-"+UUID.randomUUID(),nonce=key();var create=command(secret,nonce,null);long id=service.save(null,create);assertEquals(id,service.save(null,create));
  var row=service.detail(id);assertEquals(true,row.get("passwordConfigured"));assertFalse(row.containsKey("Psw"));assertFalse(store.encode(service.list("smtp.example.test",0,100)).contains(secret));
  String version=row.get("editToken").toString();var update=new LegacyMailSettingsService.Command("smtp2.example.test",465,"synthetic@example.test","","copy2@example.test",key(),version);assertEquals(id,service.save(id,update));assertEquals(id,service.save(id,update));assertEquals(secret,store.one("SELECT Psw FROM lin_lb_dictmailserver WHERE DictMailServerId=?",id).get("Psw"));
  assertEquals(409,assertThrows(BusinessException.class,()->service.save(id,command("replacement",key(),version))).getCode());
  var replace=command("replacement",key(),service.detail(id).get("editToken").toString());service.save(id,replace);assertEquals("replacement",store.one("SELECT Psw FROM lin_lb_dictmailserver WHERE DictMailServerId=?",id).get("Psw"));
  String audit=store.encode(store.jdbc().queryForList("SELECT before_json,after_json FROM lin_audit_record WHERE actor_user_id=? AND resource_type='LEGACY_MAIL_SETTINGS'",actor));assertFalse(audit.contains(secret));assertFalse(audit.contains("replacement"));assertFalse(audit.contains("Psw"));
  var delete=new LegacyMailSettingsService.DeleteCommand(key(),service.detail(id).get("editToken").toString());assertEquals(id,service.delete(id,delete));assertEquals(id,service.delete(id,delete));assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_dictmailserver WHERE DictMailServerId=?",Integer.class,id));
 }
 @Test void secretChangeConflictsWithSameRequestAndOutsideWritesInvalidateVersion(){
  String nonce=key();long id=service.save(null,command("first",nonce,null));assertEquals(409,assertThrows(BusinessException.class,()->service.save(null,command("other",nonce,null))).getCode());
  String version=service.detail(id).get("editToken").toString();store.jdbc().update("UPDATE lin_lb_dictmailserver SET Psw='external-synthetic-change' WHERE DictMailServerId=?",id);assertEquals(409,assertThrows(BusinessException.class,()->service.save(id,command("",key(),version))).getCode());
 }
 @Test void strictGatesAndValidation(){
  assertEquals(400,assertThrows(BusinessException.class,()->service.save(null,command("",key(),null))).getCode());
  assertEquals(400,assertThrows(BusinessException.class,()->service.save(null,new LegacyMailSettingsService.Command("host",0,"sender","secret","bcc",key(),null))).getCode());
  gate(true,false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.save(null,command("secret",key(),null))).getCode());gate(false,true,true);assertFalse(service.capabilities().get("mailSettings"));assertEquals(409,assertThrows(BusinessException.class,()->service.list("",0,20)).getCode());
  doThrow(new BusinessException(403,"denied")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.capabilities()).getCode());
 }
}
