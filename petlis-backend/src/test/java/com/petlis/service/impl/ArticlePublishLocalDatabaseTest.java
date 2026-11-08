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
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real concurrent commits in the isolated copy; no broadcast, and only exact own fixtures removed. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class ArticlePublishLocalDatabaseTest {
 @Autowired ArticleService articles;
 @Autowired FeatureStore store;
 @MockBean BusinessNotifications notifications;
 @MockBean ReviewPermission permission;
 int user;String prefix;
 @BeforeEach void setup(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));
  prefix="it_article_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
  user=(int)store.insert("INSERT INTO lin_user(group_id,username,nickname,password,salt,money,score,level,gender,successions,maxsuccessions,loginfailure,status,createtime,updatetime,jointime) VALUES(0,?,'资讯并发测试','','',0,0,0,0,0,0,0,'normal',UNIX_TIMESTAMP(),UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",prefix);
 }
 void identity(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(user,null,1,1),null,List.of()));}
 ArticleService.Article body(String title){return new ArticleService.Article(prefix,"article",title,"摘要","合成正文","",null,null,false);}
 @AfterEach void cleanup(){
  SecurityContextHolder.clearContext();
  if(user<=0)return;
  assertEquals(prefix,store.jdbc().queryForObject("SELECT username FROM lin_user WHERE id=?",String.class,user));
  store.jdbc().update("DELETE FROM lin_audit_record WHERE actor_user_id=? AND action='ARTICLE_PUBLISHED' AND resource_type='ARTICLE'",user);
  store.jdbc().update("DELETE FROM lin_article WHERE category=?",prefix);
  store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",user,prefix);
 }
 @Test void concurrentSameKeyPublishesOneArticleAndOneBroadcastAndAudit() throws Exception {
  var pool=Executors.newFixedThreadPool(4);var gate=new CountDownLatch(1);var pending=new ArrayList<Future<Long>>();
  try{
   for(int i=0;i<4;i++)pending.add(pool.submit(()->{identity();try{gate.await();return articles.publish(body(prefix),"article_publish_concurrent_123");}finally{SecurityContextHolder.clearContext();}}));
   gate.countDown();var ids=new HashSet<Long>();for(var task:pending)ids.add(task.get(20,TimeUnit.SECONDS));assertEquals(1,ids.size());
   assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_article WHERE category=?",Integer.class,prefix));
   assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id=? AND action='ARTICLE_PUBLISHED'",Integer.class,user));
   verify(notifications,times(1)).article(anyLong(),anyString(),anyString());
   identity();assertEquals(409,assertThrows(BusinessException.class,()->articles.publish(body(prefix+"changed"),"article_publish_concurrent_123")).getCode());
  }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(20,TimeUnit.SECONDS));}
 }
 @Test void invalidRequestKeyCreatesNoArticleOrBroadcast(){
  identity();assertEquals(400,assertThrows(BusinessException.class,()->articles.publish(body(prefix),"short")).getCode());
  assertEquals(0,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_article WHERE category=?",Integer.class,prefix));verifyNoInteractions(notifications);
 }
}
