package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.SecurityUtils;
import com.petlis.common.security.ReviewPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor
public class CouponService {
 private final FeatureStore store;
 private final ReviewPermission permission;
 public record Quote(Integer laboratoryId,Map<Integer,BigDecimal> prices,BigDecimal originalAmount,BigDecimal discountAmount,BigDecimal payableAmount,Long userCouponId){}
 public List<Map<String,Object>> list(String category){
  String state="CASE WHEN uc.status IN ('used','reserved') THEN uc.status WHEN c.expires_at<NOW() OR c.active<>1 THEN 'expired' WHEN c.starts_at>NOW() THEN 'upcoming' ELSE 'available' END";
  String sql="SELECT uc.id,c.name,c.amount,c.minimum_amount AS minimumAmount,c.laboratory_id AS laboratoryId,l.name AS laboratoryName,c.starts_at AS startsAt,c.expires_at AS expiresAt,"+state+" AS status FROM lin_user_coupon uc JOIN lin_coupon c ON c.id=uc.coupon_id LEFT JOIN lin_laboratory l ON l.id=c.laboratory_id WHERE uc.user_id=?";
  if(category==null || category.equals("all"))return store.jdbc().queryForList(sql+" ORDER BY uc.id DESC",SecurityUtils.getCurrentUserId());
  if(!Set.of("available","used","reserved","expired","upcoming").contains(category))throw new BusinessException("优惠券分类无效");
  return store.jdbc().queryForList(sql+" AND "+state+"=? ORDER BY uc.id DESC",SecurityUtils.getCurrentUserId(),category);
 }
 @Transactional(readOnly=true) public Quote quote(Integer laboratoryId,List<Integer> testTypeIds,Long userCouponId){return calculate(laboratoryId,testTypeIds,userCouponId,false);}
 @Transactional public Quote lockQuote(Integer laboratoryId,List<Integer> testTypeIds,Long userCouponId){return calculate(laboratoryId,testTypeIds,userCouponId,true);}
 private Quote calculate(Integer laboratoryId,List<Integer> ids,Long couponId,boolean lock){
  if(ids==null || ids.isEmpty() || ids.size()>100 || ids.stream().anyMatch(Objects::isNull) || new HashSet<>(ids).size()!=ids.size())throw new BusinessException("请选择有效且不重复的检测项目");
  if(laboratoryId==null){var labs=store.jdbc().queryForList("SELECT id FROM lin_laboratory WHERE active=1 ORDER BY sort_order,id LIMIT 2");if(labs.size()!=1)throw new BusinessException("请选择实验室；尚未配置时请联系管理员");laboratoryId=((Number)labs.get(0).get("id")).intValue();}
  store.one("SELECT id FROM lin_laboratory WHERE id=? AND active=1",laboratoryId);
  Map<Integer,BigDecimal> prices=new LinkedHashMap<>();BigDecimal total=BigDecimal.ZERO;
  for(int id:ids){var row=store.one("SELECT COALESCE(lt.price,(SELECT p.price FROM lin_lb_infotesttype_price p WHERE p.InfoTestTypeid=t.InfoTestTypeId AND p.IsActive=1 ORDER BY p.IID DESC LIMIT 1),t.Price) AS price FROM lin_lab_test lt JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=lt.test_type_id WHERE lt.laboratory_id=? AND lt.test_type_id=? AND lt.active=1 AND t.IsActive=1",laboratoryId,id);BigDecimal price=new BigDecimal(row.get("price").toString()).setScale(2,java.math.RoundingMode.HALF_UP);if(price.signum()<0)throw new BusinessException("检测项目价格无效");prices.put(id,price);total=total.add(price);}
  BigDecimal discount=BigDecimal.ZERO;
  if(couponId!=null){var c=store.one("SELECT uc.status,c.amount,c.minimum_amount,c.laboratory_id,(c.active=1 AND c.starts_at<=NOW() AND c.expires_at>=NOW()) AS usable FROM lin_user_coupon uc JOIN lin_coupon c ON c.id=uc.coupon_id WHERE uc.id=? AND uc.user_id=?"+(lock?" FOR UPDATE":""),couponId,SecurityUtils.getCurrentUserId());boolean usable=c.get("usable") instanceof Boolean b?b:((Number)c.get("usable")).intValue()==1;if(!"available".equals(c.get("status")) || !usable)throw new BusinessException("优惠券不可用或已过期");if(c.get("laboratory_id")!=null && ((Number)c.get("laboratory_id")).intValue()!=laboratoryId)throw new BusinessException("优惠券不适用于该实验室");if(total.compareTo(new BigDecimal(c.get("minimum_amount").toString()))<0)throw new BusinessException("未达到优惠券使用门槛");discount=new BigDecimal(c.get("amount").toString()).min(total);}
  return new Quote(laboratoryId,prices,total,discount,total.subtract(discount),couponId);
 }
 @Transactional public void recordOrder(int orderId,Quote q,boolean monthly){
  if(q.userCouponId()!=null && store.jdbc().update("UPDATE lin_user_coupon SET status='reserved',order_id=? WHERE id=? AND user_id=? AND status='available'",orderId,q.userCouponId(),SecurityUtils.getCurrentUserId())!=1)throw new BusinessException("优惠券已被使用，请重新核价");
  store.jdbc().update("INSERT INTO lin_order_extension(order_id,user_id,laboratory_id,user_coupon_id,original_amount,discount_amount,payable_amount,settlement_mode) VALUES(?,?,?,?,?,?,?,?)",orderId,SecurityUtils.getCurrentUserId(),q.laboratoryId(),q.userCouponId(),q.originalAmount(),q.discountAmount(),q.payableAmount(),monthly?"monthly":"cash");
 }
 @Transactional public void reviseOrder(int orderId,Quote q,boolean monthly){
  if(q.userCouponId()!=null && store.jdbc().update("UPDATE lin_user_coupon SET status='reserved',order_id=? WHERE id=? AND user_id=? AND status='available'",orderId,q.userCouponId(),SecurityUtils.getCurrentUserId())!=1)throw new BusinessException("优惠券已被使用，请重新核价");
  store.jdbc().update("INSERT INTO lin_order_extension(order_id,user_id,laboratory_id,user_coupon_id,original_amount,discount_amount,payable_amount,settlement_mode) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE laboratory_id=VALUES(laboratory_id),user_coupon_id=VALUES(user_coupon_id),original_amount=VALUES(original_amount),discount_amount=VALUES(discount_amount),payable_amount=VALUES(payable_amount),settlement_mode=VALUES(settlement_mode),review_status='pending',review_reason='',reviewed_by=NULL,reviewed_at=NULL",orderId,SecurityUtils.getCurrentUserId(),q.laboratoryId(),q.userCouponId(),q.originalAmount(),q.discountAmount(),q.payableAmount(),monthly?"monthly":"cash");
 }
 @Transactional public void consume(int orderId){store.jdbc().update("UPDATE lin_user_coupon SET status='used',used_at=NOW() WHERE order_id=? AND status='reserved'",orderId);}
 @Transactional public void release(int orderId){store.jdbc().update("UPDATE lin_user_coupon SET status='available',order_id=NULL WHERE order_id=? AND status='reserved'",orderId);}
 public record Coupon(String name,BigDecimal amount,BigDecimal minimumAmount,Integer laboratoryId,java.time.LocalDateTime startsAt,java.time.LocalDateTime expiresAt){}
 @Transactional public long create(Coupon r){permission.requireReviewer();if(r.name()==null || r.name().isBlank() || r.name().length()>100 || r.amount()==null || r.amount().signum()<=0 || r.amount().scale()>2 || r.minimumAmount()==null || r.minimumAmount().signum()<0 || r.startsAt()==null || r.expiresAt()==null || !r.expiresAt().isAfter(r.startsAt()))throw new BusinessException("优惠券规则无效");return store.insert("INSERT INTO lin_coupon(name,amount,minimum_amount,laboratory_id,starts_at,expires_at) VALUES(?,?,?,?,?,?)",r.name(),r.amount(),r.minimumAmount(),r.laboratoryId(),r.startsAt(),r.expiresAt());}
 @Transactional public long grant(long couponId,int userId){permission.requireReviewer();store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",userId);store.one("SELECT id FROM lin_coupon WHERE id=? AND active=1 AND expires_at>NOW()",couponId);return store.insert("INSERT INTO lin_user_coupon(user_id,coupon_id) VALUES(?,?)",userId,couponId);}
}
