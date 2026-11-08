package com.petlis.service.impl;

import com.petlis.common.security.*;
import com.petlis.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @Transactional
@EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyDictionaryLocalDatabaseTest {
 @Autowired FeatureStore store;
 LegacyDictionaryService service;String prefix;
 @BeforeEach void fixture(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));
  prefix="it_legacy_dict_"+UUID.randomUUID().toString().replace("-","").substring(0,10);
  store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-dictionaries',?,'transaction-only dictionary test',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'","{\"enabled\":true,\"writeEnabled\":true,\"deleteEnabled\":true}");
  var access=mock(AccessService.class);
  int actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'字典回滚测试','normal')",prefix+"_actor");
  when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));
  service=new LegacyDictionaryService(store,access);
 }
 long species(String suffix,int active,long time){return store.insert("INSERT INTO lin_lb_infoanimal(AnimalName,AnimalName_en,IsActive,createtime,updatetime) VALUES(?,?,?,?,?)",prefix+suffix,"English "+prefix+suffix,active,time,time);}
 LegacyDictionaryService.Query q(Integer active,Long from,Long to,int page,int size){return new LegacyDictionaryService.Query(prefix,null,active,from,to,null,null,"InfoAnimalId","ASC",page,size);}
 @SuppressWarnings("unchecked") List<Map<String,Object>> items(Map<String,Object> page){return (List<Map<String,Object>>)page.get("items");}
 @Test void preservesDisabledRowsRawFieldsAndInclusiveTimeFilters(){
  long disabled=species("猫",0,100),enabled=species("犬",1,200);var all=service.list("species",q(null,100L,200L,0,20));
  assertEquals(2,((Number)all.get("total")).intValue());assertEquals(2,items(all).size());var row=items(all).get(0);
  assertEquals(disabled,((Number)row.get("InfoAnimalId")).longValue());assertEquals(prefix+"猫",row.get("AnimalName"));assertEquals("English "+prefix+"猫",row.get("AnimalName_en"));assertEquals(0,((Number)row.get("IsActive")).intValue());assertEquals(100,((Number)row.get("createtime")).intValue());
  var active=service.list("species",q(1,null,null,0,20));assertEquals(1,items(active).size());assertEquals(enabled,((Number)items(active).get(0).get("InfoAnimalId")).longValue());
  assertEquals(0,((Number)service.list("species",q(null,101L,199L,0,20)).get("total")).intValue());
 }
 @Test void deterministicPagingAndParameterizedFilters(){
  long first=species("same",1,100),second=species("same",1,100);
  assertEquals(first,((Number)items(service.list("species",q(null,null,null,0,1))).get(0).get("InfoAnimalId")).longValue());
  assertEquals(second,((Number)items(service.list("species",q(null,null,null,1,1))).get(0).get("InfoAnimalId")).longValue());
  var injection=new LegacyDictionaryService.Query(prefix+"' OR 1=1 --",null,null,null,null,null,null,"InfoAnimalId","ASC",0,20);
  assertEquals(0,((Number)service.list("species",injection).get("total")).intValue());
 }
 @Test void sexReadsOriginalColumnsAndFlagIsStrictBoolean(){
  long id=store.insert("INSERT INTO lin_lb_dictsex(SexName,SexName_en,IsActive,createtime,updatetime) VALUES(?,?,0,100,100)",prefix,prefix+" EN");
  var q=new LegacyDictionaryService.Query(prefix,null,null,null,null,null,null,"DictSexId","DESC",0,20);var result=service.list("sex",q);
  assertEquals(1,items(result).size());assertEquals(id,((Number)items(result).get(0).get("DictSexId")).longValue());assertEquals(prefix+" EN",items(result).get(0).get("SexName_en"));
  store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0","{\"enabled\":\"true\"}");assertFalse(service.enabled());
 }

 String key(){return "dict_"+UUID.randomUUID().toString().replace("-","");}
 @Test void createUsesOriginalDisabledDefaultAndReplaysOneRecordAndAudit(){
  String request=key();var command=new LegacyDictionaryService.Command(prefix,"English "+prefix,null,request,null);
  long id=service.create("species",command);assertEquals(id,service.create("species",command));
  var saved=service.get("species",id);assertEquals(0,((Number)saved.get("IsActive")).intValue());assertEquals(prefix,saved.get("AnimalName"));assertNotNull(saved.get("createtime"));assertEquals(saved.get("createtime"),saved.get("updatetime"));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_DICTIONARY' AND request_id=?",Integer.class,request));
  assertEquals(409,assertThrows(BusinessException.class,()->service.create("species",new LegacyDictionaryService.Command(prefix+"other","Other",0,request,null))).getCode());
 }
 @Test void updatePreservesCreationAndOmittedStateAndRejectsStaleSnapshot(){
  long id=service.create("sex",new LegacyDictionaryService.Command(prefix,"English",1,key(),null));
  store.jdbc().update("UPDATE lin_lb_dictsex SET createtime=100,updatetime=100 WHERE DictSexId=?",id);var before=service.get("sex",id);String token=(String)before.get("editToken"),request=key();
  var command=new LegacyDictionaryService.Command(prefix+" changed","English changed",null,request,token);
  assertEquals(id,service.update("sex",id,command));assertEquals(id,service.update("sex",id,command));var after=service.get("sex",id);
  assertEquals(100,((Number)after.get("createtime")).intValue());assertTrue(((Number)after.get("updatetime")).longValue()>100);assertEquals(1,((Number)after.get("IsActive")).intValue());assertEquals(prefix+" changed",after.get("SexName"));
  assertEquals(409,assertThrows(BusinessException.class,()->service.update("sex",id,new LegacyDictionaryService.Command(prefix+" stale","Other",0,key(),token))).getCode());assertEquals(prefix+" changed",service.get("sex",id).get("SexName"));
 }
 @Test void unchangedSaveDoesNotAdvancePhpStyleUpdateTimestamp(){
  long id=species(" unchanged",0,100);var before=service.get("species",id);
  service.update("species",id,new LegacyDictionaryService.Command((String)before.get("AnimalName"),(String)before.get("AnimalName_en"),0,key(),(String)before.get("editToken")));
  assertEquals(100,((Number)service.get("species",id).get("updatetime")).intValue());
 }
 @Test void disabledWriteDoesNotChangeDictionaryOrAddAudit(){
  int before=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_DICTIONARY'",Integer.class);
  store.jdbc().update("UPDATE lin_operational_config SET config_value=? WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0","{\"enabled\":false}");
  assertEquals(409,assertThrows(BusinessException.class,()->service.create("species",new LegacyDictionaryService.Command(prefix,"English",0,key(),null))).getCode());
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_infoanimal WHERE AnimalName=?",Integer.class,prefix));assertEquals(before,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_DICTIONARY'",Integer.class));
 }

 LegacyDictionaryService.Target target(String kind,long id){return new LegacyDictionaryService.Target(id,(String)service.get(kind,id).get("editToken"));}
 @Test void bulkStatusChangesOnlyDifferentRowsAndRequestReplayDoesNotDuplicateAudit(){
  long first=species(" zero",0,100),second=species(" one",1,100);String request=key();
  var command=new LegacyDictionaryService.BatchCommand(List.of(target("species",second),target("species",first)),1,request);
  assertEquals(1,service.status("species",command));assertEquals(1,service.status("species",command));assertEquals(1,((Number)service.get("species",first).get("IsActive")).intValue());
  assertEquals(100,((Number)service.get("species",second).get("updatetime")).intValue());
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE request_id=? AND resource_type='LEGACY_DICTIONARY'",Integer.class,request));
 }
 @Test void bulkNoChangeReturnsOriginalNoRowsUpdatedError(){
  long id=species(" unchanged batch",0,100);String request=key();
  var error=assertThrows(BusinessException.class,()->service.status("species",new LegacyDictionaryService.BatchCommand(List.of(target("species",id)),0,request)));
  assertEquals("未更新任何行",error.getMessage());assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE request_id=?",Integer.class,request));
 }
 @Test void deleteIsPhysicalButReplaysAfterRowsDisappearWithoutDeletingAnythingElse(){
  long first=species(" delete first",0,100),second=species(" delete second",0,100),keep=species(" keep",0,100);String request=key();
  var one=target("species",first);var two=target("species",second);
  assertEquals(2,service.delete("species",new LegacyDictionaryService.BatchCommand(List.of(two,one),null,request)));
  assertEquals(2,service.delete("species",new LegacyDictionaryService.BatchCommand(List.of(one,two),null,request)));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_infoanimal WHERE InfoAnimalId IN (?,?)",Integer.class,first,second));assertEquals(keep,((Number)service.get("species",keep).get("InfoAnimalId")).longValue());
  var audit=store.one("SELECT before_json,after_json FROM lin_audit_record WHERE request_id=?",request);assertTrue(audit.get("before_json").toString().contains(prefix+" delete first"));
 }
 @Test void staleOrRepeatedSelectionRejectsEntireBatch(){
  long first=species(" fresh",0,100),second=species(" stale",0,100);var one=target("species",first);var two=target("species",second);
  store.jdbc().update("UPDATE lin_lb_infoanimal SET AnimalName_en='changed' WHERE InfoAnimalId=?",second);
  assertEquals(409,assertThrows(BusinessException.class,()->service.delete("species",new LegacyDictionaryService.BatchCommand(List.of(one,two),null,key()))).getCode());
  assertEquals(0,((Number)service.get("species",first).get("IsActive")).intValue());assertNotNull(service.get("species",second));
  assertEquals(400,assertThrows(BusinessException.class,()->service.status("species",new LegacyDictionaryService.BatchCommand(List.of(one,one),1,key()))).getCode());
 }
 @SuppressWarnings("unchecked")
 @Test void selectorSearchPagesAndHydratesDisabledOriginalIds(){
  long first=species(" cat",0,100),second=species(" dog",1,100);
  var searched=service.select("species",List.of(prefix),null,"AND",null,2,1,null);
  assertEquals(2,((Number)searched.get("total")).intValue());var page=(List<Map<String,Object>>)searched.get("list");
  assertEquals(second,((Number)page.get(0).get("InfoAnimalId")).longValue());assertEquals(Set.of("InfoAnimalId","AnimalName","pid"),page.get(0).keySet());
  var hydrated=service.select("species",List.of("cannotmatch"),null,"AND",List.of(second,first,second),2,1,1);
  var rows=(List<Map<String,Object>>)hydrated.get("list");assertEquals(2,rows.size());assertEquals(second,((Number)rows.get(0).get("InfoAnimalId")).longValue());assertEquals(first,((Number)rows.get(1).get("InfoAnimalId")).longValue());
  var active=service.select("species",List.of(prefix),null,null,null,1,10,1);assertEquals(1,((Number)active.get("total")).intValue());
  assertEquals(List.of(),service.select("species",null,null,null,List.of(),1,10,null).get("list"));
 }
 @SuppressWarnings("unchecked")
 @Test void selectorWordLogicAndEscapedDisplayKeepPhpMeaning(){
  long first=species(" cat",1,100),second=species(" dog",1,100);
  var result=service.select("species",List.of(prefix+" cat",prefix+" dog"),List.of("AnimalName","AnimalName_en"),"AND",null,1,10,null);
  assertEquals(2,((Number)result.get("total")).intValue());
  assertEquals(0,((Number)service.select("species",List.of(prefix+"' OR 1=1 --"),null,null,null,1,10,null).get("total")).intValue());
  store.jdbc().update("UPDATE lin_lb_infoanimal SET AnimalName=? WHERE InfoAnimalId=?",prefix+" \\u732b",first);
  var hydrated=(List<Map<String,Object>>)service.select("species",null,null,null,List.of(first),1,10,null).get("list");assertEquals(prefix+" 猫",hydrated.get(0).get("AnimalName"));
 }
 @Test void sampleTypeCrudBatchAndPickerUseOriginalTableAndDefaults(){
  String request=key();long id=service.create("sampleType",new LegacyDictionaryService.Command(prefix,"English "+prefix,null,request,null));
  assertEquals(id,service.create("sampleType",new LegacyDictionaryService.Command(prefix,"English "+prefix,null,request,null)));
  assertEquals(0,((Number)service.get("sampleType",id).get("IsActive")).intValue());
  assertEquals(1,((Number)service.select("sampleType",List.of(prefix),null,null,null,1,10,null).get("total")).intValue());
  var before=service.get("sampleType",id);
  service.update("sampleType",id,new LegacyDictionaryService.Command(prefix+" changed","English "+prefix,null,key(),(String)before.get("editToken")));
  assertEquals(1,service.status("sampleType",new LegacyDictionaryService.BatchCommand(List.of(target("sampleType",id)),1,key())));
  assertEquals(1,service.delete("sampleType",new LegacyDictionaryService.BatchCommand(List.of(target("sampleType",id)),null,key())));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_infosampletype WHERE InfoSampleTypeId=?",Integer.class,id));
 }
 @Test void varietyLifecycleDoesNotInventStatusOrAnimalRelation(){
  String request=key();var command=new LegacyDictionaryService.Command(prefix,"English "+prefix,null,request,null);
  long id=service.create("variety",command);assertEquals(id,service.create("variety",command));
  var saved=service.get("variety",id);assertFalse(saved.containsKey("IsActive"));assertEquals(prefix,saved.get("Variety_CN"));
  var query=new LegacyDictionaryService.Query(prefix,null,null,null,null,null,null,"IID","ASC",0,10);
  assertEquals(1,((Number)service.list("variety",query).get("total")).intValue());
  service.update("variety",id,new LegacyDictionaryService.Command(prefix+" changed","English "+prefix,null,key(),(String)saved.get("editToken")));
  assertEquals(prefix+" changed",service.get("variety",id).get("Variety_CN"));
  assertEquals(1,((Number)service.select("variety",null,null,null,List.of(id),1,10,null).get("total")).intValue());
  assertEquals(400,assertThrows(BusinessException.class,()->service.status("variety",new LegacyDictionaryService.BatchCommand(List.of(target("variety",id)),1,key()))).getCode());
  assertEquals(400,assertThrows(BusinessException.class,()->service.create("variety",new LegacyDictionaryService.Command(prefix,"English",0,key(),null))).getCode());
  assertEquals(1,service.delete("variety",new LegacyDictionaryService.BatchCommand(List.of(target("variety",id)),null,key())));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lb_infovariety WHERE IID=?",Integer.class,id));
 }
}
