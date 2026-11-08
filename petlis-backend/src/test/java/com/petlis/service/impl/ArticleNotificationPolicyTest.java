package com.petlis.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlis.common.security.ReviewPermission;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ArticleNotificationPolicyTest {
 @Test void unavailableArticlesAreLoggedSkippedWithoutExternalSend(){
  for(var source:List.of(List.<Map<String,Object>>of(),List.<Map<String,Object>>of(Map.of("status","archived","visible",1)),List.<Map<String,Object>>of(Map.of("status","draft","visible",1)),List.<Map<String,Object>>of(Map.of("status","published","visible",0)))){
   var jdbc=mock(JdbcTemplate.class);var store=mock(FeatureStore.class);var push=mock(SubscriptionPush.class);when(store.jdbc()).thenReturn(jdbc);
   var event=event();when(jdbc.queryForList(contains("ORDER BY id LIMIT 1"))).thenReturn(List.of(event));when(jdbc.queryForList(contains("FROM lin_article"),eq(5L))).thenReturn(source);when(jdbc.queryForList(contains("FOR UPDATE SKIP LOCKED"),eq(1L))).thenReturn(List.of(event));
   var service=service(store,push);assertEquals(1,service.process(1));verifyNoInteractions(push);verify(jdbc).update(contains("INSERT INTO lin_notification_delivery"),eq(1L),eq("skipped"),eq("CONTENT_UNAVAILABLE"),anyString());
   var order=inOrder(jdbc);order.verify(jdbc).queryForList(contains("FROM lin_article"),eq(5L));order.verify(jdbc).queryForList(contains("FOR UPDATE SKIP LOCKED"),eq(1L));
  }
 }
 @Test void publishedArticleUsesPreferenceAndSendsOnlyAfterClaim(){
  var jdbc=mock(JdbcTemplate.class);var store=mock(FeatureStore.class);var push=mock(SubscriptionPush.class);when(store.jdbc()).thenReturn(jdbc);var event=event();
  when(jdbc.queryForList(contains("ORDER BY id LIMIT 1"))).thenReturn(List.of(event));when(jdbc.queryForList(contains("FROM lin_article"),eq(5L))).thenReturn(List.of(Map.of("status","published","visible",1)));when(jdbc.queryForList(contains("FOR UPDATE SKIP LOCKED"),eq(1L))).thenReturn(List.of(event));
  when(store.one(contains("u.openid"),eq(7))).thenReturn(Map.of("openid","fixture","status","normal","news_notifications",1));when(push.send(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(new SubscriptionPush.Outcome("sent","OK","",false));
  assertEquals(1,service(store,push).process(1));verify(push,times(1)).send(eq("article"),eq("fixture"),anyString(),anyString(),eq("pages/article-detail/article-detail?id=5"));
 }
 private Map<String,Object> event(){return Map.of("id",1L,"user_id",7,"event_type","article","business_key","article:5","page_path","pages/article-detail/article-detail?id=5","title","活动","message","摘要","retry_count",0);}
 private BusinessNotifications service(FeatureStore store,SubscriptionPush push){
  var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
  return new BusinessNotifications(store,push,mock(ReviewPermission.class),new TransactionTemplate(manager),new MockEnvironment(),new ObjectMapper());
 }
}
