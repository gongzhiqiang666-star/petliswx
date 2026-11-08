package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class LegacyTestCatalogLocalDatabaseTest {
 @Autowired FeatureStore store;LegacyTestCatalogService service;int actor;String prefix;
 @BeforeEach void setup(){assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_legacy_test_"+UUID.randomUUID().toString().substring(0,8);actor=(int)store.insert("INSERT INTO lin_user(username,nickname,status) VALUES(?,'项目迁移测试','normal')",prefix);var access=mock(AccessService.class);when(access.current()).thenReturn(new CurrentActor(actor,Set.of(PlatformRole.ADMIN),Set.of(),Set.of(),new DataScope(true,Set.of(),Set.of(),null)));service=new LegacyTestCatalogService(store,access);gate(true,true);}
 void gate(boolean enabled,boolean writable){store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-test-catalog',?,'transaction-only test catalog',?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'",store.encode(Map.of("enabled",enabled,"writeEnabled",writable)),actor);}
 String key(){return "catalog_"+UUID.randomUUID().toString().replace("-","");}
 long create(){return service.create(new LegacyTestCatalogService.ItemCommand(prefix,prefix,null,null,key(),null));}
 @Test void globalItemCreationPreservesPhpCallbackAndFormDefaults(){
  long id=create();var row=service.item(id);for(String field:List.of("Price","CtoG","GtoC","DiscountReal","DiscountNext"))assertEquals(0,new BigDecimal(row.get(field).toString()).signum());
  assertEquals("",row.get("TextModel"));assertEquals(0,((Number)row.get("type")).intValue());assertEquals(1,((Number)row.get("IsActive")).intValue());assertNotNull(row.get("CreateDt"));assertNull(row.get("createtime"));assertNull(row.get("updatetime"));assertEquals("无",row.get("DealPrice"));
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_lab_test WHERE test_type_id=?",Integer.class,id));
 }
 @Test void multiplePriceRowsKeepIndependentStateAndNoInventedTimestamps(){
  long id=create();var cmd=new LegacyTestCatalogService.PriceCommand(new BigDecimal("125.50"),null,key());long price=service.addPrice(id,cmd);assertEquals(price,service.addPrice(id,cmd));
  service.addPrice(id,new LegacyTestCatalogService.PriceCommand(new BigDecimal("99.00"),0,key()));assertEquals(2,((Number)service.prices(id,0,20).get("total")).intValue());
  var row=store.one("SELECT * FROM lin_lb_infotesttype_price WHERE IID=?",price);assertEquals(1,((Number)row.get("IsActive")).intValue());assertNull(row.get("createtime"));assertNull(row.get("updatetime"));
  String display=String.valueOf(service.item(id).get("DealPrice"));assertTrue(display.contains("125.50"));assertTrue(display.contains("99.00"));assertEquals(0,new BigDecimal(service.item(id).get("Price").toString()).signum());
 }
 @Test void editingIdentityDoesNotOverwritePricesOrDefaultsAndRejectsStaleSnapshot(){
  long id=create();var before=service.item(id);String token=(String)before.get("editToken");service.update(id,new LegacyTestCatalogService.ItemCommand(prefix+"改",prefix+"X",2,0,key(),token));var after=service.item(id);assertEquals(prefix+"改",after.get("TestName"));assertEquals(2,((Number)after.get("type")).intValue());assertEquals(before.get("CreateDt"),after.get("CreateDt"));assertEquals(before.get("Price"),after.get("Price"));assertEquals(409,assertThrows(BusinessException.class,()->service.update(id,new LegacyTestCatalogService.ItemCommand(prefix,prefix,0,1,key(),token))).getCode());
 }
 @Test void gatesPreventDirectWritesAndRequestReuseWithChangedPayload(){
  String request=key();long id=service.create(new LegacyTestCatalogService.ItemCommand(prefix,prefix,0,1,request,null));assertEquals(id,service.create(new LegacyTestCatalogService.ItemCommand(prefix,prefix,0,1,request,null)));
  assertEquals(409,assertThrows(BusinessException.class,()->service.create(new LegacyTestCatalogService.ItemCommand(prefix+"other",prefix,0,1,request,null))).getCode());gate(true,false);assertEquals(409,assertThrows(BusinessException.class,()->service.addPrice(id,new LegacyTestCatalogService.PriceCommand(BigDecimal.ONE,1,key()))).getCode());gate(false,true);assertEquals(409,assertThrows(BusinessException.class,()->service.item(id)).getCode());
 }
 @SuppressWarnings("unchecked") @Test void priceApiPreservesDecimalTextAndTrailingZeroesWithoutJavascriptPrecisionLoss(){
  long id=create();service.addPrice(id,new LegacyTestCatalogService.PriceCommand(new BigDecimal("90071992547409.91"),1,key()));
  var rows=(List<Map<String,Object>>)service.prices(id,0,20).get("items");assertEquals("90071992547409.91",rows.get(0).get("price"));
 }
 @Test void priceMaintenanceKeepsSnapshotAndTimestampsAndRequiresExplicitDeleteGate(){
  long item=create();long price=service.addPrice(item,new LegacyTestCatalogService.PriceCommand(new BigDecimal("125.50"),1,key()));
  var before=service.price(item,price);String token=(String)before.get("editToken");String request=key();
  var change=new LegacyTestCatalogService.PriceEditCommand(new BigDecimal("150.00"),0,request,token);
  assertEquals(price,service.updatePrice(item,price,change));assertEquals(price,service.updatePrice(item,price,change));
  var after=service.price(item,price);assertEquals("150.00",after.get("price"));assertEquals(0,((Number)after.get("IsActive")).intValue());assertNull(after.get("createtime"));assertNull(after.get("updatetime"));
  assertEquals(409,assertThrows(BusinessException.class,()->service.updatePrice(item,price,new LegacyTestCatalogService.PriceEditCommand(BigDecimal.ONE,1,key(),token))).getCode());
  assertThrows(BusinessException.class,()->service.price(item+1,price));
  var deletion=new LegacyTestCatalogService.PriceDeleteCommand(key(),(String)after.get("editToken"));
  assertEquals(409,assertThrows(BusinessException.class,()->service.deletePrice(item,price,deletion)).getCode());
  store.jdbc().update("UPDATE lin_operational_config SET config_value=JSON_SET(config_value,'$.deleteEnabled',true) WHERE config_key='legacy-test-catalog' AND scope_type='GLOBAL' AND scope_id=0");
  assertEquals(price,service.deletePrice(item,price,deletion));assertEquals(price,service.deletePrice(item,price,deletion));
  assertEquals(404,assertThrows(BusinessException.class,()->service.price(item,price)).getCode());assertEquals("无",service.item(item).get("DealPrice"));
 }
 @Test void originalEditAllowsFourDecimalInputAndOldColumnRoundsToTwo(){long item=create();long price=service.addPrice(item,new LegacyTestCatalogService.PriceCommand(BigDecimal.ONE,1,key()));var before=service.price(item,price);service.updatePrice(item,price,new LegacyTestCatalogService.PriceEditCommand(new BigDecimal("125.5555"),null,key(),(String)before.get("editToken")));var after=service.price(item,price);assertEquals("125.56",after.get("price"));service.updatePrice(item,price,new LegacyTestCatalogService.PriceEditCommand(new BigDecimal("-125.5555"),null,key(),(String)after.get("editToken")));assertEquals("-125.56",service.price(item,price).get("price"));}
 @Test void quoteIsReadOnlyAndKeepsOriginalFactorTruncation(){
  int before=store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=?",Integer.class,actor);
  assertEquals("75.00",service.quote(new LegacyTestCatalogService.QuoteCommand("100.00","7.55")));
  assertEquals("1.00",service.quote(new LegacyTestCatalogService.QuoteCommand("1.005","原价")));
  assertEquals("125.50",service.quote(new LegacyTestCatalogService.QuoteCommand("125.50","其他折扣")));
  assertEquals(before,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=?",Integer.class,actor));
  gate(false,false);assertEquals(409,assertThrows(BusinessException.class,()->service.quote(new LegacyTestCatalogService.QuoteCommand("100","9"))).getCode());
 }
}
