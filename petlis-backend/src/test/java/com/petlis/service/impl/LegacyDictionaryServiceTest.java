package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class LegacyDictionaryServiceTest {
 FeatureStore store;JdbcTemplate jdbc;AccessService access;LegacyDictionaryService service;
 @BeforeEach void setup(){store=mock(FeatureStore.class);jdbc=mock(JdbcTemplate.class);access=mock(AccessService.class);when(store.jdbc()).thenReturn(jdbc);service=new LegacyDictionaryService(store,access);}
 void enable(){when(jdbc.queryForList(contains("lin_operational_config"),eq("legacy-dictionaries"))).thenReturn(List.of(Map.of("enabledType","BOOLEAN","enabled","true","writeType","BOOLEAN","writeEnabled","true")));}
 LegacyDictionaryService.Query query(String sort){return new LegacyDictionaryService.Query("猫","Cat",0,1L,2L,null,null,sort,"DESC",0,20);}
 @Test void missingOrStringFlagNeverEnablesMigration(){assertFalse(service.enabled());when(jdbc.queryForList(anyString(),eq("legacy-dictionaries"))).thenReturn(List.of(Map.of("enabledType","STRING","enabled","true")));assertFalse(service.enabled());assertEquals(409,assertThrows(BusinessException.class,()->service.list("species",query("InfoAnimalId"))).getCode());verify(store,never()).page(anyString(),anyString(),anyInt(),anyInt(),any(Object[].class));}
 @Test void directAccessStillRequiresAdministrator(){doThrow(new BusinessException(403,"无权访问")).when(access).requireAdmin();assertEquals(403,assertThrows(BusinessException.class,()->service.list("species",query("InfoAnimalId"))).getCode());verifyNoInteractions(jdbc);}
 @Test void originalSpeciesFieldsAndFilterMeaningArePreserved(){enable();when(store.page(anyString(),anyString(),anyInt(),anyInt(),any(Object[].class))).thenReturn(Map.of("items",List.of(),"total",0));service.list("species",query("InfoAnimalId"));verify(store).page(contains("lin_lb_infoanimal"),contains("AnimalName,AnimalName_en,IsActive,createtime,updatetime"),eq(0),eq(20),eq("%猫%"),eq("%Cat%"),eq(0),eq(1L),eq(2L));}
 @Test void sexUsesItsOwnTableAndIdentifiers(){enable();service.list("sex",new LegacyDictionaryService.Query(null,null,null,null,null,null,null,"DictSexId","ASC",1,10));verify(store).page(contains("lin_lb_dictsex"),contains("ORDER BY DictSexId ASC"),eq(1),eq(10));}
 @Test void tableAndSortInjectionAreRejected(){enable();assertEquals(400,assertThrows(BusinessException.class,()->service.list("lin_user",query("InfoAnimalId"))).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.list("species",query("InfoAnimalId; DROP TABLE lin_user"))).getCode());verify(store,never()).page(anyString(),anyString(),anyInt(),anyInt(),any(Object[].class));}
 @Test void reversedRangeAndInvalidOrderAreRejected(){enable();assertThrows(BusinessException.class,()->service.list("species",new LegacyDictionaryService.Query(null,null,null,2L,1L,null,null,"InfoAnimalId","ASC",0,20)));assertThrows(BusinessException.class,()->service.list("species",new LegacyDictionaryService.Query(null,null,null,null,null,null,null,"InfoAnimalId","other",0,20)));}
 @Test void schemaKeepsChineseLabelsAndOriginalDisabledDefault(){enable();var schemas=service.schemas();assertEquals(5,schemas.size());var species=schemas.get(0);assertEquals("物种",species.get("label"));assertEquals(0,species.get("createDefaultIsActive"));assertEquals(Map.of("0","禁用","1","正常"),species.get("statusLabels"));assertTrue(species.toString().contains("物种名称(英文)"));}

 @Test void writesCannotBypassDisabledModule(){assertEquals(409,assertThrows(BusinessException.class,()->service.create("species",new LegacyDictionaryService.Command("猫","Cat",null,"test_dictionary_123456",null))).getCode());verify(store,never()).insert(anyString(),any(Object[].class));}
 @Test void requiredNamesAndDatabaseLengthsAreEnforced(){enable();assertEquals(400,assertThrows(BusinessException.class,()->service.create("sex",new LegacyDictionaryService.Command(" ","Male",1,"test_dictionary_123456",null))).getCode());assertEquals(400,assertThrows(BusinessException.class,()->service.create("sex",new LegacyDictionaryService.Command("男".repeat(51),"Male",1,"test_dictionary_123456",null))).getCode());verify(store,never()).insert(anyString(),any(Object[].class));}
 @Test void malformedRequestKeyCannotCreateOrAudit(){enable();assertEquals(400,assertThrows(BusinessException.class,()->service.create("species",new LegacyDictionaryService.Command("猫","Cat",0,"bad",null))).getCode());verify(store,never()).insert(anyString(),any(Object[].class));}

 @Test void readOnlyFlagDoesNotImplicitlyGrantDictionaryWrites(){
  when(jdbc.queryForList(contains("lin_operational_config"),eq("legacy-dictionaries"))).thenReturn(List.of(Map.of("enabledType","BOOLEAN","enabled","true")));
  assertTrue(service.enabled());assertEquals(409,assertThrows(BusinessException.class,()->service.create("species",new LegacyDictionaryService.Command("猫","Cat",0,"test_dictionary_123456",null))).getCode());verify(store,never()).insert(anyString(),any(Object[].class));
 }

 @Test void writeSwitchMustBeBooleanAndCannotOverrideDisabledReadModule(){
  when(jdbc.queryForList(contains("lin_operational_config"),eq("legacy-dictionaries"))).thenReturn(List.of(Map.of("enabledType","BOOLEAN","enabled","true","writeType","STRING","writeEnabled","true")));
  assertEquals(Map.of("dictionaries",true,"dictionaryWrites",false,"dictionaryDeletes",false),service.capabilities());
  when(jdbc.queryForList(contains("lin_operational_config"),eq("legacy-dictionaries"))).thenReturn(List.of(Map.of("enabledType","BOOLEAN","enabled","false","writeType","BOOLEAN","writeEnabled","true")));
  assertEquals(Map.of("dictionaries",false,"dictionaryWrites",false,"dictionaryDeletes",false),service.capabilities());
 }

 @Test void dictionaryDeleteRequiresSeparateExplicitPermission(){enable();assertEquals(409,assertThrows(BusinessException.class,()->service.delete("species",new LegacyDictionaryService.BatchCommand(List.of(new LegacyDictionaryService.Target(1,"a".repeat(64))),null,"batch_dictionary_123456"))).getCode());verify(jdbc,never()).update(startsWith("DELETE"),any(Object[].class));}
 @Test void bulkStatusRejectsEmptyTargetsBeforeAnyMutation(){enable();assertEquals(400,assertThrows(BusinessException.class,()->service.status("species",new LegacyDictionaryService.BatchCommand(List.of(),1,"batch_dictionary_123456"))).getCode());verify(store,never()).insert(anyString(),any(Object[].class));}

 @Test void deleteSwitchMustBeBooleanAndCannotBypassWriteSwitch(){
  when(jdbc.queryForList(contains("lin_operational_config"),eq("legacy-dictionaries"))).thenReturn(List.of(Map.of("enabledType","BOOLEAN","enabled","true","writeType","BOOLEAN","writeEnabled","true","deleteType","STRING","deleteEnabled","true")));
  assertEquals(false,service.capabilities().get("dictionaryDeletes"));
  when(jdbc.queryForList(contains("lin_operational_config"),eq("legacy-dictionaries"))).thenReturn(List.of(Map.of("enabledType","BOOLEAN","enabled","true","writeType","BOOLEAN","writeEnabled","false","deleteType","BOOLEAN","deleteEnabled","true")));
  assertEquals(false,service.capabilities().get("dictionaryDeletes"));
 }
 @Test void selectorCannotBypassReadSwitch(){assertEquals(409,assertThrows(BusinessException.class,()->service.select("species",null,null,null,null,1,10,null)).getCode());}
 @Test void selectorHydratesDisabledRowsInRequestedOrderWithoutLeakingOtherFields(){
  enable();when(jdbc.queryForList(startsWith("SELECT InfoAnimalId,AnimalName FROM"),any(Object[].class))).thenReturn(List.of(Map.of("InfoAnimalId",1,"AnimalName","猫"),Map.of("InfoAnimalId",3,"AnimalName","犬")));
  var result=service.select("species",List.of("missing"),null,null,List.of(3L,1L,3L),5,1,1);
  assertEquals(List.of(Map.of("InfoAnimalId",3,"AnimalName","犬","pid",0),Map.of("InfoAnimalId",1,"AnimalName","猫","pid",0)),result.get("list"));assertEquals(2,result.get("total"));
  verify(jdbc).queryForList("SELECT InfoAnimalId,AnimalName FROM lin_lb_infoanimal WHERE InfoAnimalId IN (?,?)",3L,1L);
 }
 @Test void sampleTypeSchemaUsesOriginalLabelsAndFields(){
  enable();var schema=service.schemas().stream().filter(r->"sampleType".equals(r.get("kind"))).findFirst().orElseThrow();
  assertEquals("采样类型",schema.get("label"));assertEquals(250,schema.get("maxNameLength"));assertTrue(schema.toString().contains("添加时间"));
  service.list("sampleType",new LegacyDictionaryService.Query(null,null,null,null,null,null,null,"InfoSampleTypeId","ASC",0,10));
  verify(store).page(contains("lin_lb_infosampletype"),contains("InfoSampleTypeId,SampleName,SampleName_en,IsActive,createtime,updatetime"),eq(0),eq(10));
 }
 @Test void varietySchemaHasNoInventedStatusOrAnimalRelation(){
  enable();var schema=service.schemas().stream().filter(r->"variety".equals(r.get("kind"))).findFirst().orElseThrow();
  assertEquals("品种",schema.get("label"));assertEquals(false,schema.get("hasStatus"));assertEquals(50,schema.get("maxNameLength"));
  assertFalse(schema.toString().contains("IsActive"));assertTrue(schema.toString().contains("中文名称"));assertTrue(schema.toString().contains("英文名称"));
  service.list("variety",new LegacyDictionaryService.Query(null,null,null,null,null,null,null,"IID","ASC",0,10));
  verify(store).page(contains("lin_lb_infovariety"),contains("IID,Variety_CN,Variety_EN,createtime,updatetime"),eq(0),eq(10));
  assertEquals(400,assertThrows(BusinessException.class,()->service.list("variety",new LegacyDictionaryService.Query(null,null,1,null,null,null,null,"IID","ASC",0,10))).getCode());
 }
}
