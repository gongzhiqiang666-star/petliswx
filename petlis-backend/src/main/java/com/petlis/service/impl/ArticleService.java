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
 public Map<String,Object> popup(){var rows=store.jdbc().queryForList("SELECT id,title,summary,cover_url AS coverUrl FROM lin_article WHERE popup_enabled=1 AND status='published' AND published_at<=NOW() ORDER BY published_at DESC LIMIT 1");return rows.isEmpty()?Collections.emptyMap():rows.get(0);}
 public record Article(String category,String type,String title,String summary,String content,String coverUrl,String liveAt,String liveUrl,boolean popupEnabled){}
 @Transactional public long publish(Article r){permission.requireReviewer();if(r.category()==null || r.category().isBlank() || r.category().length()>100 || r.title()==null || r.title().isBlank() || r.title().length()>250 || r.content()==null || r.content().length()>1000000 || !Set.of("article","live","activity").contains(r.type()))throw new BusinessException("资讯内容无效");if("live".equals(r.type()) && (r.liveAt()==null || r.liveUrl()==null || !r.liveUrl().startsWith("https://")))throw new BusinessException("直播时间和链接不完整");long id=store.insert("INSERT INTO lin_article(category,type,title,summary,content,cover_url,live_at,live_url,popup_enabled,status,published_at) VALUES(?,?,?,?,?,?,?,?,?,'published',NOW())",r.category(),r.type(),r.title(),r.summary()==null?"":r.summary(),r.content(),r.coverUrl()==null?"":r.coverUrl(),r.liveAt(),r.liveUrl()==null?"":r.liveUrl(),r.popupEnabled());notifications.article(id,r.title(),r.summary()==null?"有新的资讯发布":r.summary());store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id) VALUES(?,?,?,?)",SecurityUtils.getCurrentUserId(),"ARTICLE_PUBLISHED","ARTICLE",String.valueOf(id));return id;}
}
