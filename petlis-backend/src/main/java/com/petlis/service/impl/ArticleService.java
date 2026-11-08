package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import com.petlis.common.security.ReviewPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor
public class ArticleService {
 private final BusinessNotifications notifications;

 private final FeatureStore store;
 private final ReviewPermission permission;
 public Map<String,Object> list(String category,boolean bookmarked,int page,int size){
  String where=" FROM lin_article a LEFT JOIN lin_article_action ua ON ua.article_id=a.id AND ua.user_id=? WHERE a.status='published' AND a.published_at<=NOW()";
  List<Object> args=new ArrayList<>();args.add(SecurityUtils.getCurrentUserId());
  if(category!=null && !category.isBlank()){where+=" AND a.category=?";args.add(category);}
  if(bookmarked)where+=" AND ua.bookmarked=1";
  return store.page("SELECT COUNT(*)"+where,"SELECT a.id,a.category,a.type,a.title,a.summary,a.cover_url AS coverUrl,a.live_at AS liveAt,a.published_at AS publishedAt,COALESCE(ua.liked,0) AS liked,COALESCE(ua.bookmarked,0) AS bookmarked"+where+" ORDER BY a.published_at DESC,a.id DESC",page,size,args.toArray());
 }
 public Map<String,Object> detail(long id){return store.one("SELECT a.*,COALESCE(ua.liked,0) AS liked,COALESCE(ua.bookmarked,0) AS bookmarked FROM lin_article a LEFT JOIN lin_article_action ua ON ua.article_id=a.id AND ua.user_id=? WHERE a.id=? AND a.status='published' AND a.published_at<=NOW()",SecurityUtils.getCurrentUserId(),id);}
 @Transactional public void action(long id,boolean liked,boolean bookmarked){detail(id);store.jdbc().update("INSERT INTO lin_article_action(user_id,article_id,liked,bookmarked) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE liked=VALUES(liked),bookmarked=VALUES(bookmarked)",SecurityUtils.getCurrentUserId(),id,liked,bookmarked);}
 public List<Map<String,Object>> categories(){return store.jdbc().queryForList("SELECT DISTINCT category FROM lin_article WHERE status='published' AND published_at<=NOW() ORDER BY category");}
 public Map<String,Object> popup(){
  int hours=24;
  var settings=store.jdbc().queryForList("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) AS enabled,JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.minimumIntervalHours')) AS minimumIntervalHours FROM lin_operational_config WHERE config_key='article-popup' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");
  if(!settings.isEmpty()){
   var setting=settings.get(0);if(setting.get("enabled")!=null && !"true".equals(setting.get("enabled").toString()))return Collections.emptyMap();
   if(setting.get("minimumIntervalHours")!=null)try{hours=Integer.parseInt(setting.get("minimumIntervalHours").toString());if(hours<1 || hours>720)return Collections.emptyMap();}catch(NumberFormatException e){return Collections.emptyMap();}
  }
  var rows=store.jdbc().queryForList("SELECT id,title,summary,cover_url AS coverUrl FROM lin_article WHERE popup_enabled=1 AND status='published' AND published_at<=NOW() ORDER BY published_at DESC,id DESC LIMIT 1");
  if(rows.isEmpty())return Collections.emptyMap();var result=new LinkedHashMap<String,Object>(rows.get(0));result.put("minimumIntervalHours",hours);return result;
 }
 public record Article(String category,String type,String title,String summary,String content,String coverUrl,String liveAt,String liveUrl,boolean popupEnabled){}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public long publish(Article r){return publish(r,null);}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public long publish(Article r,String requestKey){
  permission.requireReviewer();validate(r);
  String hash=null;
  if(requestKey!=null){
   if(!requestKey.matches("[A-Za-z0-9_-]{16,128}"))throw new BusinessException(400,"发布请求编号无效");
   hash=payloadHash(r);
   int actor=SecurityUtils.getCurrentUserId();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",actor);
   var prior=store.jdbc().queryForList("SELECT a.resource_id,JSON_UNQUOTE(JSON_EXTRACT(a.after_json,'$.payloadHash')) AS payloadHash,p.id AS articleId FROM lin_audit_record a LEFT JOIN lin_article p ON p.id=CAST(a.resource_id AS UNSIGNED) WHERE a.actor_user_id=? AND a.action='ARTICLE_PUBLISHED' AND a.resource_type='ARTICLE' AND a.request_id=? FOR UPDATE",actor,requestKey);
   if(!prior.isEmpty()){
    if(prior.size()!=1 || !hash.equals(prior.get(0).get("payloadHash")) || prior.get(0).get("articleId")==null)throw new BusinessException(409,"原发布请求内容已变化或记录不完整，请核对原资讯");
    return Long.parseLong(prior.get(0).get("resource_id").toString());
   }
  }
  String liveAt=r.liveAt()==null || r.liveAt().isBlank()?null:r.liveAt().replace('T',' ');
  long id=store.insert("INSERT INTO lin_article(category,type,title,summary,content,cover_url,live_at,live_url,popup_enabled,status,published_at) VALUES(?,?,?,?,?,?,?,?,?,'published',NOW())",r.category(),r.type(),r.title(),r.summary()==null?"":r.summary(),r.content(),r.coverUrl()==null?"":r.coverUrl(),liveAt,r.liveUrl()==null?"":r.liveUrl(),r.popupEnabled());
  notifications.article(id,r.title(),r.summary()==null?"有新的资讯发布":r.summary());
  if(requestKey==null)store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id) VALUES(?,?,?,?)",SecurityUtils.getCurrentUserId(),"ARTICLE_PUBLISHED","ARTICLE",String.valueOf(id));
  else store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id,after_json) VALUES(?,?,?,?,?,?)",SecurityUtils.getCurrentUserId(),"ARTICLE_PUBLISHED","ARTICLE",String.valueOf(id),requestKey,store.encode(Map.of("payloadHash",hash)));
  return id;
 }
 void validateDraft(Article r){validate(r,false);}
 void validate(Article r){validate(r,true);}
 private void validate(Article r,boolean publishing){
  if(r==null || r.category()==null || r.category().isBlank() || r.category().length()>100 || r.title()==null || r.title().isBlank() || r.title().length()>250 || r.content()==null || publishing && r.content().isBlank() || r.content().length()>1000000 || r.type()==null || !Set.of("article","live","activity").contains(r.type()) || r.summary()!=null && r.summary().length()>1000)throw new BusinessException(400,"资讯分类、标题、摘要或正文无效");
  if(!optionalHttps(r.coverUrl()) || !optionalHttps(r.liveUrl()))throw new BusinessException(400,"封面和直播链接须为有效HTTPS地址，且不超过1000字符");
  if(publishing && "live".equals(r.type()) && (r.liveAt()==null || r.liveAt().isBlank() || r.liveUrl()==null || r.liveUrl().isBlank()))throw new BusinessException(400,"直播时间和链接不完整");
  if(r.liveAt()!=null && !r.liveAt().isBlank())try{java.time.LocalDateTime.parse(r.liveAt().replace(' ','T'));}catch(java.time.format.DateTimeParseException e){throw new BusinessException(400,"直播时间无效，请填写真实日期和时间");}
 }
 private boolean optionalHttps(String value){
  if(value==null || value.isBlank())return true;
  if(value.length()>1000)return false;
  try{var uri=java.net.URI.create(value);return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost()!=null && !uri.getHost().isBlank() && uri.getUserInfo()==null;}catch(IllegalArgumentException e){return false;}
 }
 private String payloadHash(Article article){
  try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(store.encode(article).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException("发布请求校验不可用");}
 }
}
