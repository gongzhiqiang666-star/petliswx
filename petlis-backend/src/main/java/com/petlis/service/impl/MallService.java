package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.ReviewPermission;
import com.petlis.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class MallService {
 private final BusinessNotifications notifications;

 private final FeatureStore store;
 private final ReviewPermission permission;
 private final com.fasterxml.jackson.databind.ObjectMapper json;
 public Map<String,Object> gifts(int page,int size){return store.page("SELECT COUNT(*) FROM lin_gift WHERE active=1","SELECT id,name,description,image_url AS imageUrl,points_cost AS pointsCost,stock FROM lin_gift WHERE active=1 ORDER BY id DESC",page,size);}
 public Map<String,Object> gift(long id){return store.one("SELECT id,name,description,image_url AS imageUrl,points_cost AS pointsCost,stock FROM lin_gift WHERE id=? AND active=1",id);}
 public record Address(String recipient,String phone,String address){}
 public record Redemption(long giftId,int quantity,Address deliveryAddress,String requestKey){}
 @Transactional public long redeem(Redemption r){
  if(r.quantity()<1 || r.quantity()>99 || r.requestKey()==null || !r.requestKey().matches("[A-Za-z0-9_-]{16,64}") || r.deliveryAddress()==null)throw new BusinessException("兑换请求无效");
  var a=r.deliveryAddress();if(a.recipient()==null || a.recipient().isBlank() || a.recipient().length()>50 || a.phone()==null || !a.phone().matches("[0-9+ -]{7,20}") || a.address()==null || a.address().isBlank() || a.address().length()>500)throw new BusinessException("请填写完整收货地址");
  int userId=SecurityUtils.getCurrentUserId();
  var user=store.one("SELECT score FROM lin_user WHERE id=? FOR UPDATE",userId);
  var existing=store.jdbc().queryForList("SELECT id,gift_id,quantity,delivery_address FROM lin_redemption WHERE user_id=? AND request_key=?",userId,r.requestKey());
  if(!existing.isEmpty()){
   var first=existing.get(0);if(((Number)first.get("gift_id")).longValue()!=r.giftId() || ((Number)first.get("quantity")).intValue()!=r.quantity())throw new BusinessException(409,"重复请求的兑换内容不一致");
   try{if(!json.readTree(first.get("delivery_address").toString()).equals(json.valueToTree(a)))throw new BusinessException(409,"重复请求的收货信息不一致");}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new BusinessException(409,"原兑换信息无法校验");}
   return ((Number)first.get("id")).longValue();
  }
  var gift=store.one("SELECT id,name,points_cost,stock,active FROM lin_gift WHERE id=? FOR UPDATE",r.giftId());
  int cost;try{cost=Math.multiplyExact(((Number)gift.get("points_cost")).intValue(),r.quantity());}catch(ArithmeticException e){throw new BusinessException("兑换数量无效");}
  int before=((Number)user.get("score")).intValue();
  if(((Number)gift.get("active")).intValue()!=1 || ((Number)gift.get("stock")).intValue()<r.quantity())throw new BusinessException("礼品已下架或库存不足");
  if(before<cost)throw new BusinessException("积分不足");
  store.jdbc().update("UPDATE lin_user SET score=score-?,updatetime=UNIX_TIMESTAMP() WHERE id=?",cost,userId);
  store.jdbc().update("UPDATE lin_gift SET stock=stock-? WHERE id=?",r.quantity(),r.giftId());
  long id=store.insert("INSERT INTO lin_redemption(user_id,gift_id,gift_name,quantity,points_spent,delivery_address,request_key) VALUES(?,?,?,?,?,?,?)",userId,r.giftId(),gift.get("name"),r.quantity(),cost,store.encode(a),r.requestKey());
  store.jdbc().update("INSERT INTO lin_user_score_log(user_id,score,`before`,`after`,memo,createtime) VALUES(?,?,?,?,?,UNIX_TIMESTAMP())",userId,-cost,before,before-cost,"礼品兑换 #"+id);
  notifications.emit(userId,"mall","redemption:"+id,"礼品兑换成功","兑换申请 #"+id+" 已提交，消耗 "+cost+" 积分。","pages/mall/mall");
  return id;
 }
 public Map<String,Object> history(int page,int size){return store.page("SELECT COUNT(*) FROM lin_redemption WHERE user_id=?","SELECT id,gift_id AS giftId,gift_name AS giftName,quantity,points_spent AS pointsSpent,status,tracking_number AS trackingNumber,created_at AS createdAt FROM lin_redemption WHERE user_id=? ORDER BY id DESC",page,size,SecurityUtils.getCurrentUserId());}
 public record Gift(String name,String description,String imageUrl,int pointsCost,int stock){}
 @Transactional public long createGift(Gift r){permission.requireReviewer();if(r.name()==null || r.name().isBlank() || r.name().length()>200 || r.pointsCost()<1 || r.stock()<0)throw new BusinessException("礼品信息无效");return store.insert("INSERT INTO lin_gift(name,description,image_url,points_cost,stock) VALUES(?,?,?,?,?)",r.name(),r.description(),r.imageUrl()==null?"":r.imageUrl(),r.pointsCost(),r.stock());}
}
