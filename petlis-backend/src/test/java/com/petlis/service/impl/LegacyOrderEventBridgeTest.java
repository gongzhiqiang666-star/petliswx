package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LegacyOrderEventBridgeTest {
 @TempDir Path directory;
 @Test void initialBaselineIsSavedAndReusedAcrossRestarts() throws Exception {
  var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject(anyString(),eq(Long.class))).thenReturn(12L);
  var env=new MockEnvironment().withProperty("app.notifications.legacy-baseline-file",directory.resolve("baseline").toString());
  var store=new FeatureStore(jdbc,new ObjectMapper());var notifications=mock(BusinessNotifications.class);
  new LegacyOrderEventBridge(store,notifications,env).initialize();
  assertEquals("12",Files.readString(directory.resolve("baseline")));
  clearInvocations(jdbc);var restarted=new LegacyOrderEventBridge(store,notifications,env);restarted.initialize();verifyNoInteractions(jdbc);
  when(jdbc.queryForList(anyString(),eq(12L))).thenReturn(List.of());assertEquals(0,restarted.importBatch());
  verify(jdbc).queryForList(contains("e.id IS NULL"),eq(12L));
 }
 @Test void importedMessagesUseStatusMappingAndDeterministicKeys() throws Exception {
  var jdbc=mock(JdbcTemplate.class);var notifications=mock(BusinessNotifications.class);var file=directory.resolve("baseline");Files.writeString(file,"0");
  when(jdbc.queryForList(anyString(),eq(0L))).thenReturn(List.of(Map.of("id",20,"order_id",9,"new_status",-3,"user_id",7),Map.of("id",21,"order_id",9,"new_status",3,"user_id",7)));
  var bridge=new LegacyOrderEventBridge(new FeatureStore(jdbc,new ObjectMapper()),notifications,new MockEnvironment().withProperty("app.notifications.legacy-baseline-file",file.toString()));bridge.initialize();assertEquals(2,bridge.importBatch());
  verify(notifications).emit(eq(7),eq("order"),eq("legacy:20"),contains("待支付"),contains("待支付"),eq("pages/order-detail/order-detail?id=9"));
  verify(notifications).emit(eq(7),eq("report"),eq("legacy:21"),contains("已完成"),contains("已完成"),anyString());
  verify(jdbc).queryForList(contains("creator.hospital_id=n.hospital_id"),eq(0L));
 }
 @Test void proactivePublishedEventSuppressesLegacyDuplicate() throws Exception {
  var jdbc=mock(JdbcTemplate.class);var notifications=mock(BusinessNotifications.class);var file=directory.resolve("baseline");Files.writeString(file,"0");
  when(jdbc.queryForList(anyString(),eq(0L))).thenReturn(List.of(Map.of("id",21,"order_id",9,"new_status",3,"user_id",7)));
  when(jdbc.queryForObject(contains("business_key"),eq(Boolean.class),eq("9:published"))).thenReturn(true);
  var bridge=new LegacyOrderEventBridge(new FeatureStore(jdbc,new ObjectMapper()),notifications,new MockEnvironment().withProperty("app.notifications.legacy-baseline-file",file.toString()));bridge.initialize();
  assertEquals(0,bridge.importBatch());
  verify(notifications,never()).emit(anyInt(),any(),any(),any(),any(),any());
 }
}
