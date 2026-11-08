package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Editorial lifecycle using existing article and audit tables; no destructive deletion. */
@Service @RequiredArgsConstructor
@Transactional(isolation=Isolation.READ_COMMITTED)
public class ArticleAdminService {
 private final FeatureStore store;
 private final ReviewPermission permission;
 private final ArticleService articles;
 private final BusinessNotifications notifications;
 public record Edit(ArticleService.Article article,long version,String reason,String requestKey){}
 public record Change(String status,long version,String reason,String requestKey){}
 public Map<String,Object> list(String status,int page,int size){
  permission.requireReviewer();if(status!=null && !status.isBlank() && !Set.of("draft","published","archived").contains(status))throw new BusinessException(400,"资讯状态无效");
  String where=status==null || status.isBlank()?"":" WHERE status=?";Object[] args=where.isEmpty()?new Object[]{}:new Object[]{status};
  return store.page("SELECT COUNT(*) FROM lin_article"+where,"SELECT id,category,type,title,status,published_at AS publishedAt,updated_at AS updatedAt FROM lin_article"+where+" ORDER BY updated_at DESC,id DESC",page,size,args);
 }
 public Map<String,Object> detail(long id){permission.requireReviewer();return detailInternal(id);}
 private Map<String,Object> detailInternal(long id){var row=new LinkedHashMap<>(store.one("SELECT * FROM lin_article WHERE id=? FOR UPDATE",id));row.put("version",version(id));return row;}
 private long version(long id){Long v=store.jdbc().queryForObject("SELECT COALESCE(MAX(id),0) FROM lin_audit_record WHERE resource_type='ARTICLE' AND CAST(resource_id AS UNSIGNED)=?",Long.class,id);return v==null?0:v;}
 public long draft(ArticleService.Article article,String key){
  permission.requireReviewer();articles.validateDraft(article);String hash=hash(article);int actor=lockActor(key);
  Long replay=replay(actor,"ARTICLE_DRAFT_CREATED",key,hash,null);if(replay!=null)return replay;
  long id=store.insert("INSERT INTO lin_article(category,type,title,summary,content,cover_url,live_at,live_url,popup_enabled,status) VALUES(?,?,?,?,?,?,?,?,?,'draft')",values(article));
  audit(actor,"ARTICLE_DRAFT_CREATED",id,key,hash,null,article);return id;
 }
 public void edit(long id,Edit command){
  permission.requireReviewer();if(command==null)throw new BusinessException(400,"编辑内容无效");articles.validateDraft(command.article());reason(command.reason());
  String hash=hash(command);int actor=lockActor(command.requestKey());if(replay(actor,"ARTICLE_UPDATED",command.requestKey(),hash,id)!=null)return;
  var before=locked(id,command.version());if("published".equals(before.get("status")))articles.validate(command.article());
  Object[] fields=values(command.article()),args=Arrays.copyOf(fields,10);args[9]=id;
  store.jdbc().update("UPDATE lin_article SET category=?,type=?,title=?,summary=?,content=?,cover_url=?,live_at=?,live_url=?,popup_enabled=? WHERE id=?",args);
  audit(actor,"ARTICLE_UPDATED",id,command.requestKey(),hash,before,Map.of("article",command.article(),"reason",command.reason().trim()));
 }
 public void change(long id,Change command){
  permission.requireReviewer();if(command==null || command.status()==null || !Set.of("draft","published","archived").contains(command.status()))throw new BusinessException(400,"目标状态无效");reason(command.reason());
  String hash=hash(command);int actor=lockActor(command.requestKey());if(replay(actor,"ARTICLE_STATUS_CHANGED",command.requestKey(),hash,id)!=null)return;
  var before=locked(id,command.version());String current=String.valueOf(before.get("status"));
  boolean allowed=("draft".equals(current) && Set.of("published","archived").contains(command.status())) || ("published".equals(current) && "archived".equals(command.status())) || ("archived".equals(current) && "draft".equals(command.status()));
  if(!allowed)throw new BusinessException(409,"资讯状态已变化，请刷新；下架内容需先恢复草稿再发布");
  if("published".equals(command.status())){
   var article=from(before);articles.validate(article);
   store.jdbc().update("UPDATE lin_article SET status='published',published_at=COALESCE(published_at,NOW()) WHERE id=?",id);
   // The existing article business key keeps re-publication from duplicating each recipient's event.
   notifications.article(id,article.title(),article.summary());
  }else {store.jdbc().update("UPDATE lin_article SET status=? WHERE id=?",command.status(),id);if("archived".equals(command.status()))notifications.withdrawArticle(id);}
  audit(actor,"ARTICLE_STATUS_CHANGED",id,command.requestKey(),hash,before,Map.of("status",command.status(),"reason",command.reason().trim()));
 }
 private Map<String,Object> locked(long id,long expected){
  var row=store.one("SELECT * FROM lin_article WHERE id=? FOR UPDATE",id);
  if(expected<0 || version(id)!=expected)throw new BusinessException(409,"资讯已被其他操作修改，请刷新后重新核对");return row;
 }
 private int lockActor(String key){
  if(key==null || !key.matches("[A-Za-z0-9_-]{16,128}"))throw new BusinessException(400,"操作请求编号无效");int actor=SecurityUtils.getCurrentUserId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);return actor;
 }
 private Long replay(int actor,String action,String key,String hash,Long expectedId){
  var prior=store.jdbc().queryForList("SELECT resource_id,JSON_UNQUOTE(JSON_EXTRACT(after_json,'$.payloadHash')) AS payloadHash FROM lin_audit_record WHERE actor_user_id=? AND action=? AND resource_type='ARTICLE' AND request_id=? FOR UPDATE",actor,action,key);
  if(prior.isEmpty())return null;
  if(prior.size()!=1 || !hash.equals(prior.get(0).get("payloadHash")))throw new BusinessException(409,"原请求内容不一致，请核对操作记录");
  long id=Long.parseLong(prior.get(0).get("resource_id").toString());if(expectedId!=null && id!=expectedId)throw new BusinessException(409,"请求编号已用于其他资讯");
  store.one("SELECT id FROM lin_article WHERE id=?",id);return id;
 }
 private void audit(int actor,String action,long id,String key,String hash,Object before,Object after){
  store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,before_json,after_json) VALUES(?,?,?,?,?,?,?)",actor,action,"ARTICLE",String.valueOf(id),key,before==null?null:store.encode(before),store.encode(Map.of("payloadHash",hash,"value",after)));
 }
 private void reason(String reason){if(reason==null || reason.isBlank() || reason.length()>1000)throw new BusinessException(400,"请填写不超过1000字的操作原因");}
 private Object[] values(ArticleService.Article a){return new Object[]{a.category(),a.type(),a.title(),a.summary()==null?"":a.summary(),a.content(),a.coverUrl()==null?"":a.coverUrl(),a.liveAt()==null || a.liveAt().isBlank()?null:a.liveAt().replace('T',' '),a.liveUrl()==null?"":a.liveUrl(),a.popupEnabled()};}
 private ArticleService.Article from(Map<String,Object> row){return new ArticleService.Article(text(row,"category"),text(row,"type"),text(row,"title"),text(row,"summary"),text(row,"content"),text(row,"cover_url"),row.get("live_at")==null?null:String.valueOf(row.get("live_at")),text(row,"live_url"),((Number)row.get("popup_enabled")).intValue()==1);}
 private String text(Map<String,Object> row,String key){return Objects.toString(row.get(key),"");}
 private String hash(Object value){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(store.encode(value).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException("操作校验不可用");}}
}
