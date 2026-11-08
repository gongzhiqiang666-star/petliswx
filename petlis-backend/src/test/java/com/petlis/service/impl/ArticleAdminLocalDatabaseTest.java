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
@SpringBootTest @EnabledIfEnvironmentVariable(named="PETLIS_LOCAL_DB_TEST",matches="true")
class ArticleAdminLocalDatabaseTest {
 @Autowired ArticleAdminService admin;
 @Autowired ArticleService reader;
 @Autowired FeatureStore store;
 @MockBean BusinessNotifications notifications;
 @MockBean ReviewPermission permission;
 int first,second;String prefix;
 @BeforeEach void setup(){
  assertTrue(System.getenv("DB_URL").startsWith("jdbc:mysql://127.0.0.1:13306/laboklin?"));prefix="it_editor_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
  first=user(prefix+"a");second=user(prefix+"b");identity(first);
 }
 int user(String name){return (int)store.insert("INSERT INTO lin_user(group_id,username,nickname,password,salt,money,score,level,gender,successions,maxsuccessions,loginfailure,status,createtime,updatetime,jointime) VALUES(0,?,'资讯管理回归','','',0,0,0,0,0,0,0,'normal',UNIX_TIMESTAMP(),UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",name);}
 void identity(int id){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(id,null,1,1),null,List.of()));}
 ArticleService.Article body(String title){return new ArticleService.Article(prefix,"article",title,"摘要","正文","",null,null,false);}
 long revision(long id){return ((Number)admin.detail(id).get("version")).longValue();}
 String key(){return "editor_"+UUID.randomUUID().toString().replace("-","");}
 @AfterEach void cleanup(){
  SecurityContextHolder.clearContext();
  if(prefix==null)return;
  for(var row:store.jdbc().queryForList("SELECT id FROM lin_article WHERE category=?",prefix))store.jdbc().update("DELETE FROM lin_audit_record WHERE resource_type='ARTICLE' AND resource_id=?",row.get("id").toString());
  store.jdbc().update("DELETE FROM lin_article WHERE category=?",prefix);
  for(int id:new int[]{first,second})if(id>0){String name=store.jdbc().queryForObject("SELECT username FROM lin_user WHERE id=?",String.class,id);assertTrue(name.startsWith(prefix));store.jdbc().update("DELETE FROM lin_user WHERE id=? AND username=?",id,name);}
 }
 @Test void draftEditReplayAndStaleWritePreserveOneVersion(){
  String creation=key();long id=admin.draft(body("草稿"),creation);assertEquals(id,admin.draft(body("草稿"),creation));verifyNoInteractions(notifications);
  assertEquals("draft",admin.detail(id).get("status"));assertEquals(404,assertThrows(BusinessException.class,()->reader.detail(id)).getCode());
  long version=revision(id);var edit=new ArticleAdminService.Edit(body("已编辑"),version,"修正文案",key());admin.edit(id,edit);admin.edit(id,edit);
  assertEquals("已编辑",admin.detail(id).get("title"));assertTrue(revision(id)>version);
  assertEquals(409,assertThrows(BusinessException.class,()->admin.edit(id,new ArticleAdminService.Edit(body("旧页面"),version,"过期编辑",key()))).getCode());
  assertEquals(1,store.jdbc().queryForObject("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='ARTICLE' AND resource_id=? AND action='ARTICLE_UPDATED'",Integer.class,Long.toString(id)));
 }
 @Test void publicationAndWithdrawalControlReaderVisibilityAndReplay(){
  long id=admin.draft(body("待发布"),key());var publish=new ArticleAdminService.Change("published",revision(id),"确认发布",key());admin.change(id,publish);admin.change(id,publish);
  assertEquals("待发布",reader.detail(id).get("title"));verify(notifications,times(1)).article(eq(id),anyString(),anyString());
  var archive=new ArticleAdminService.Change("archived",revision(id),"内容过期下架",key());admin.change(id,archive);admin.change(id,archive);
  assertEquals(404,assertThrows(BusinessException.class,()->reader.detail(id)).getCode());assertEquals("archived",admin.detail(id).get("status"));assertNotNull(admin.detail(id).get("published_at"));
  assertEquals(409,assertThrows(BusinessException.class,()->admin.change(id,new ArticleAdminService.Change("published",revision(id),"跳过草稿",key()))).getCode());
  admin.change(id,new ArticleAdminService.Change("draft",revision(id),"恢复草稿",key()));verify(notifications,times(1)).article(anyLong(),anyString(),anyString());
 }
 @Test void twoEditorsSameVersionOnlyOneSucceeds() throws Exception {
  long id=admin.draft(body("原始"),key()),version=revision(id);var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);var futures=new ArrayList<Future<Integer>>();
  try{
   for(int actor:new int[]{first,second})futures.add(pool.submit(()->{identity(actor);try{gate.await();admin.edit(id,new ArticleAdminService.Edit(body("编辑"+actor),version,"并发核对",key()));return 200;}catch(BusinessException e){return e.getCode();}finally{SecurityContextHolder.clearContext();}}));
   gate.countDown();var outcomes=new ArrayList<Integer>();for(var f:futures)outcomes.add(f.get(20,TimeUnit.SECONDS));Collections.sort(outcomes);assertEquals(List.of(200,409),outcomes);
  }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(20,TimeUnit.SECONDS));}
 }
}
