package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class ArticleNotificationLocalDatabaseTest {
 @Autowired ArticleAdminService admin;
 @Autowired FeatureStore store;
 @Autowired BusinessNotifications notifications;
 @MockBean SubscriptionPush push;
 @MockBean ReviewPermission permission;
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void archiveSkipsOnlyOwnPendingNoticeLogsOnceAndBlocksRetry(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));String prefix="it_withdraw_"+UUID.randomUUID().toString().replace("-","").substring(0,10);
  int actor=(int)store.insert("INSERT INTO lin_user(group_id,username,nickname,password,salt,money,score,level,gender,successions,maxsuccessions,loginfailure,status,createtime,updatetime,jointime) VALUES(0,?,'下架通知回归','','',0,0,0,0,0,0,0,'normal',UNIX_TIMESTAMP(),UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",prefix);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(actor,null,1,1),null,List.of()));
  long article=admin.draft(new ArticleService.Article(prefix,"article","合成资讯","摘要","正文","",null,null,false),"withdraw_draft_"+UUID.randomUUID().toString().replace("-",""));
  // Only own fixture publication state; no broadcast to other local copied accounts.
  store.jdbc().update("UPDATE lin_article SET status='published',published_at=NOW() WHERE id=?",article);
  notifications.emit(actor,"article","article:"+article,"合成资讯","摘要","pages/article-detail/article-detail?id="+article);
  notifications.emit(actor,"order",prefix,"对照订单","摘要","pages/orders/orders");
  long event=store.jdbc().queryForObject("SELECT id FROM lin_notification_event WHERE user_id=? AND event_type='article' AND business_key=?",Long.class,actor,"article:"+article);
  long version=((Number)admin.detail(article).get("version")).longValue();var change=new ArticleAdminService.Change("archived",version,"测试下架阻断推送","withdraw_archive_"+UUID.randomUUID().toString().replace("-",""));
  admin.change(article,change);admin.change(article,change);notifications.withdrawArticle(article);
  assertEquals("skipped",store.jdbc().queryForObject("SELECT push_status FROM lin_notification_event WHERE id=?",String.class,event));
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_notification_delivery WHERE event_id=? AND error_code='CONTENT_UNAVAILABLE'",Integer.class,event));
  assertEquals("pending",store.jdbc().queryForObject("SELECT push_status FROM lin_notification_event WHERE user_id=? AND event_type='order' AND business_key=?",String.class,actor,prefix));
  assertEquals(409,assertThrows(BusinessException.class,()->notifications.retry(event)).getCode());
  verifyNoInteractions(push);
 }
}
