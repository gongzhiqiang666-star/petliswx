package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @RequiredArgsConstructor
public class DraftService {
 private final FeatureStore store;
 public record Draft(Integer laboratoryId,Map<String,Object> payload,Integer version){}
 public List<Map<String,Object>> list(){return store.jdbc().queryForList("SELECT id,laboratory_id AS laboratoryId,JSON_UNQUOTE(JSON_EXTRACT(payload,'$.petInfo.animalName')) AS animalName,version,updated_at AS updatedAt FROM lin_order_draft WHERE user_id=? ORDER BY updated_at DESC LIMIT 100",SecurityUtils.getCurrentUserId());}
 public Map<String,Object> get(long id){return store.one("SELECT id,laboratory_id AS laboratoryId,payload,version,updated_at AS updatedAt FROM lin_order_draft WHERE id=? AND user_id=?",id,SecurityUtils.getCurrentUserId());}
 private String validate(Draft r){if(r.payload()==null)throw new BusinessException("草稿内容不能为空");String value=store.encode(r.payload());if(value.length()>65536)throw new BusinessException("草稿内容过大");if(r.laboratoryId()!=null)store.one("SELECT id FROM lin_laboratory WHERE id=? AND active=1",r.laboratoryId());return value;}
 @Transactional public long create(Draft r){String value=validate(r);return store.insert("INSERT INTO lin_order_draft(user_id,laboratory_id,payload) VALUES(?,?,?)",SecurityUtils.getCurrentUserId(),r.laboratoryId(),value);}
 @Transactional public void update(long id,Draft r){String value=validate(r);if(r.version()==null)throw new BusinessException("请刷新草稿后重试");int changed=store.jdbc().update("UPDATE lin_order_draft SET laboratory_id=?,payload=?,version=version+1 WHERE id=? AND user_id=? AND version=?",r.laboratoryId(),value,id,SecurityUtils.getCurrentUserId(),r.version());if(changed!=1)throw new BusinessException(409,"草稿已更新或不存在，请重新加载");}
 @Transactional public void validateForSubmission(long id,Integer version){
  var rows=store.jdbc().queryForList("SELECT version FROM lin_order_draft WHERE id=? AND user_id=? FOR UPDATE",id,SecurityUtils.getCurrentUserId());
  if(version==null || rows.isEmpty() || ((Number)rows.get(0).get("version")).intValue()!=version)throw new BusinessException(409,"草稿已提交或已更新，请查看订单记录后重试");
 }
 @Transactional public void consume(long id,Integer version){if(version==null || store.jdbc().update("DELETE FROM lin_order_draft WHERE id=? AND user_id=? AND version=?",id,SecurityUtils.getCurrentUserId(),version)!=1)throw new BusinessException(409,"草稿已提交或已更新，请查看订单记录后重试");}
 @Transactional public void delete(long id){if(store.jdbc().update("DELETE FROM lin_order_draft WHERE id=? AND user_id=?",id,SecurityUtils.getCurrentUserId())!=1)throw new BusinessException(404,"草稿不存在");}
}
