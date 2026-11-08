package com.petlis.service.impl;
import com.petlis.common.security.ReviewPermission;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ArticlePopupPolicyTest {
 @Test void popupDefaultIs24HoursAndReadsOnlyPublishedContent(){
  var store=mock(FeatureStore.class);var jdbc=mock(JdbcTemplate.class);when(store.jdbc()).thenReturn(jdbc);
  when(jdbc.queryForList(contains("FROM lin_article"))).thenReturn(List.of(Map.of("id",7L)));
  var service=new ArticleService(mock(BusinessNotifications.class),store,mock(ReviewPermission.class));assertEquals(24,service.popup().get("minimumIntervalHours"));
  verify(jdbc).queryForList(contains("status='published' AND published_at<=NOW()"));
 }
 @Test void disabledOrMalformedPolicySuppressesPopupAndValidIntervalIsReturned(){
  for(var settings:List.of(Map.of("enabled","false"),Map.of("minimumIntervalHours","0"),Map.of("minimumIntervalHours","721"),Map.of("minimumIntervalHours","broken"))){
   var store=mock(FeatureStore.class);var jdbc=mock(JdbcTemplate.class);when(store.jdbc()).thenReturn(jdbc);when(jdbc.queryForList(contains("lin_operational_config"))).thenReturn(List.of(new HashMap<>(settings)));
   assertTrue(new ArticleService(mock(BusinessNotifications.class),store,mock(ReviewPermission.class)).popup().isEmpty());verify(jdbc,never()).queryForList(contains("FROM lin_article"));
  }
  var store=mock(FeatureStore.class);var jdbc=mock(JdbcTemplate.class);when(store.jdbc()).thenReturn(jdbc);when(jdbc.queryForList(contains("lin_operational_config"))).thenReturn(List.of(Map.of("enabled","true","minimumIntervalHours","6")));when(jdbc.queryForList(contains("FROM lin_article"))).thenReturn(List.of(Map.of("id",7L)));
  assertEquals(6,new ArticleService(mock(BusinessNotifications.class),store,mock(ReviewPermission.class)).popup().get("minimumIntervalHours"));
 }
}
