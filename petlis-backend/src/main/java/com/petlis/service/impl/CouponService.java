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
 private final ContractService contracts;
 public record Quote(Integer laboratoryId,Map<Integer,BigDecimal> prices,BigDecimal originalAmount,BigDecimal discountAmount,BigDecimal payableAmount,Long userCouponId,
                     @com.fasterxml.jackson.annotation.JsonIgnore Long contractId,@com.fasterxml.jackson.annotation.JsonIgnore Boolean monthly,@com.fasterxml.jackson.annotation.JsonIgnore Map<Integer,Long> contractPriceIds){
  public Quote(Integer laboratoryId,Map<Integer,BigDecimal> prices,BigDecimal originalAmount,BigDecimal discountAmount,BigDecimal payableAmount,Long userCouponId){this(laboratoryId,prices,originalAmount,discountAmount,payableAmount,userCouponId,null,null,Map.of());}
 }
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
  ContractService.Resolution resolution=contracts.priceForOrder(SecurityUtils.getCurrentHospitalId(),laboratoryId,ids);
  Map<Integer,Long> contractPriceIds=new LinkedHashMap<>();
  if(resolution.contracted())for(int id:ids){var item=resolution.items().get(id);prices.put(id,item.finalPrice());contractPriceIds.put(id,item.id());total=total.add(item.finalPrice());}
  else for(int id:ids){var row=store.one("SELECT COALESCE(lt.price,(SELECT p.price FROM lin_lb_infotesttype_price p WHERE p.InfoTestTypeid=t.InfoTestTypeId AND p.IsActive=1 ORDER BY p.IID DESC LIMIT 1),t.Price) AS price FROM lin_lab_test lt JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=lt.test_type_id WHERE lt.laboratory_id=? AND lt.test_type_id=? AND lt.active=1 AND t.IsActive=1",laboratoryId,id);BigDecimal price=new BigDecimal(row.get("price").toString()).setScale(2,java.math.RoundingMode.HALF_UP);if(price.signum()<0)throw new BusinessException("检测项目价格无效");prices.put(id,price);total=total.add(price);}
  BigDecimal discount=BigDecimal.ZERO;
  if(couponId!=null){var c=store.one("SELECT uc.status,c.amount,c.minimum_amount,c.laboratory_id,(c.active=1 AND c.starts_at<=NOW() AND c.expires_at>=NOW()) AS usable FROM lin_user_coupon uc JOIN lin_coupon c ON c.id=uc.coupon_id WHERE uc.id=? AND uc.user_id=?"+(lock?" FOR UPDATE":""),couponId,SecurityUtils.getCurrentUserId());boolean usable=c.get("usable") instanceof Boolean b?b:((Number)c.get("usable")).intValue()==1;if(!"available".equals(c.get("status")) || !usable)throw new BusinessException("优惠券不可用或已过期");if(c.get("laboratory_id")!=null && ((Number)c.get("laboratory_id")).intValue()!=laboratoryId)throw new BusinessException("优惠券不适用于该实验室");if(total.compareTo(new BigDecimal(c.get("minimum_amount").toString()))<0)throw new BusinessException("未达到优惠券使用门槛");discount=new BigDecimal(c.get("amount").toString()).min(total);}
  return new Quote(laboratoryId,prices,total,discount,total.subtract(discount),couponId,resolution.contracted()?resolution.contractId():null,resolution.contracted()?"MONTHLY".equals(resolution.settlementType()):null,contractPriceIds);
 }
 @Transactional public void recordOrder(int orderId,Quote q,boolean monthly){
  if(q.userCouponId()!=null && store.jdbc().update("UPDATE lin_user_coupon SET status='reserved',order_id=? WHERE id=? AND user_id=? AND status='available'",orderId,q.userCouponId(),SecurityUtils.getCurrentUserId())!=1)throw new BusinessException("优惠券已被使用，请重新核价");
  store.jdbc().update("INSERT INTO lin_order_extension(order_id,user_id,laboratory_id,user_coupon_id,original_amount,discount_amount,payable_amount,settlement_mode) VALUES(?,?,?,?,?,?,?,?)",orderId,SecurityUtils.getCurrentUserId(),q.laboratoryId(),q.userCouponId(),q.originalAmount(),q.discountAmount(),q.payableAmount(),Boolean.TRUE.equals(q.monthly()) || q.monthly()==null && monthly?"monthly":"cash");
  recordPricingSnapshot(orderId,q,monthly);
 }
 @Transactional public void reviseOrder(int orderId,Quote q,boolean monthly){
  if(q.userCouponId()!=null && store.jdbc().update("UPDATE lin_user_coupon SET status='reserved',order_id=? WHERE id=? AND user_id=? AND status='available'",orderId,q.userCouponId(),SecurityUtils.getCurrentUserId())!=1)throw new BusinessException("优惠券已被使用，请重新核价");
  store.jdbc().update("INSERT INTO lin_order_extension(order_id,user_id,laboratory_id,user_coupon_id,original_amount,discount_amount,payable_amount,settlement_mode) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE laboratory_id=VALUES(laboratory_id),user_coupon_id=VALUES(user_coupon_id),original_amount=VALUES(original_amount),discount_amount=VALUES(discount_amount),payable_amount=VALUES(payable_amount),settlement_mode=VALUES(settlement_mode),review_status='pending',review_reason='',reviewed_by=NULL,reviewed_at=NULL",orderId,SecurityUtils.getCurrentUserId(),q.laboratoryId(),q.userCouponId(),q.originalAmount(),q.discountAmount(),q.payableAmount(),Boolean.TRUE.equals(q.monthly()) || q.monthly()==null && monthly?"monthly":"cash");
  recordPricingSnapshot(orderId,q,monthly);
 }
 private void recordPricingSnapshot(int orderId,Quote q,boolean legacyMonthly){
  String settlement=Boolean.TRUE.equals(q.monthly()) || q.monthly()==null && legacyMonthly?"monthly":"cash";
  var items=q.prices().entrySet().stream().map(entry->{var item=new LinkedHashMap<String,Object>();item.put("testTypeId",entry.getKey());item.put("contractItemPriceId",q.contractPriceIds().get(entry.getKey()));item.put("unitPrice",entry.getValue());return item;}).toList();
  store.jdbc().update("INSERT INTO lin_order_pricing_snapshot(order_id,contract_id,hospital_id,laboratory_id,settlement_type,original_amount,discount_amount,payable_amount,items_json) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE contract_id=VALUES(contract_id),hospital_id=VALUES(hospital_id),laboratory_id=VALUES(laboratory_id),settlement_type=VALUES(settlement_type),original_amount=VALUES(original_amount),discount_amount=VALUES(discount_amount),payable_amount=VALUES(payable_amount),items_json=VALUES(items_json)",orderId,q.contractId(),SecurityUtils.getCurrentHospitalId(),q.laboratoryId(),settlement,q.originalAmount(),q.discountAmount(),q.payableAmount(),store.encode(items));
 }
 public boolean isMonthly(int orderId,boolean legacyMonthly){var rows=store.jdbc().queryForList("SELECT settlement_mode FROM lin_order_extension WHERE order_id=?",orderId);return rows.isEmpty()?legacyMonthly:"monthly".equals(rows.get(0).get("settlement_mode"));}
 @Transactional public void consume(int orderId){store.jdbc().update("UPDATE lin_user_coupon SET status='used',used_at=NOW() WHERE order_id=? AND status='reserved'",orderId);}
 @Transactional public void release(int orderId){store.jdbc().update("UPDATE lin_user_coupon SET status='available',order_id=NULL WHERE order_id=? AND status='reserved'",orderId);}
 public record Coupon(String name,BigDecimal amount,BigDecimal minimumAmount,Integer laboratoryId,java.time.LocalDateTime startsAt,java.time.LocalDateTime expiresAt){}
 @Transactional public long create(Coupon r){permission.requireReviewer();validateCoupon(r);long id=store.insert("INSERT INTO lin_coupon(name,amount,minimum_amount,laboratory_id,starts_at,expires_at) VALUES(?,?,?,?,?,?)",r.name(),r.amount(),r.minimumAmount(),r.laboratoryId(),r.startsAt(),r.expiresAt());audit("COUPON_CREATED","COUPON",id,null);return id;}
 @Transactional public long grant(long couponId,int userId,String requestKey){
  permission.requireReviewer();if(requestKey==null || !requestKey.matches("[A-Za-z0-9_-]{8,100}"))throw new BusinessException("发放请求编号无效");
  var existing=store.jdbc().queryForList("SELECT resource_id FROM lin_audit_record WHERE action='COUPON_GRANTED' AND request_id=?",requestKey);if(!existing.isEmpty())return Long.parseLong(existing.get(0).get("resource_id").toString());
  store.one("SELECT id FROM lin_user WHERE id=? FOR UPDATE",userId);
  existing=store.jdbc().queryForList("SELECT resource_id FROM lin_audit_record WHERE action='COUPON_GRANTED' AND request_id=?",requestKey);if(!existing.isEmpty())return Long.parseLong(existing.get(0).get("resource_id").toString());
  store.one("SELECT id FROM lin_coupon WHERE id=? AND active=1 AND expires_at>NOW() FOR UPDATE",couponId);
  var stockRow=store.one("SELECT config_value FROM lin_operational_config WHERE config_key=? AND scope_type='GLOBAL' AND scope_id=0 FOR UPDATE","coupon-stock:"+couponId);
  int[] stock=readStock(stockRow.get("config_value").toString());if(stock[1]>=stock[0])throw new BusinessException("优惠券库存不足");
  long id=store.insert("INSERT INTO lin_user_coupon(user_id,coupon_id) VALUES(?,?)",userId,couponId);
  store.jdbc().update("UPDATE lin_operational_config SET config_value=?,updated_by=? WHERE config_key=?","{\"total\":"+stock[0]+",\"issued\":"+(stock[1]+1)+"}",SecurityUtils.getCurrentUserId(),"coupon-stock:"+couponId);
  audit("COUPON_GRANTED","USER_COUPON",id,requestKey);return id;
 }
 public Map<String,Object> templates(int page,int size){permission.requireReviewer();return store.page("SELECT COUNT(*) FROM lin_coupon","SELECT c.*,COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(oc.config_value,'$.total')) AS UNSIGNED),0) AS totalQuantity,COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(oc.config_value,'$.issued')) AS UNSIGNED),0) AS issuedQuantity FROM lin_coupon c LEFT JOIN lin_operational_config oc ON oc.config_key=CONCAT('coupon-stock:',c.id) AND oc.scope_type='GLOBAL' AND oc.scope_id=0 ORDER BY c.id DESC",page,size);}
 @Transactional public void updateCoupon(long id,Coupon r,int totalQuantity,String reason){
  permission.requireReviewer();validateCoupon(r);if(totalQuantity<0)throw new BusinessException("优惠券库存无效");store.one("SELECT id FROM lin_coupon WHERE id=? FOR UPDATE",id);
  var stocks=store.jdbc().queryForList("SELECT config_value FROM lin_operational_config WHERE config_key=?","coupon-stock:"+id);int issued=stocks.isEmpty()?0:readStock(stocks.get(0).get("config_value").toString())[1];if(totalQuantity<issued)throw new BusinessException("发行总量不得低于已发数量");
  store.jdbc().update("UPDATE lin_coupon SET name=?,amount=?,minimum_amount=?,laboratory_id=?,starts_at=?,expires_at=? WHERE id=?",r.name(),r.amount(),r.minimumAmount(),r.laboratoryId(),r.startsAt(),r.expiresAt(),id);
  store.jdbc().update("INSERT INTO lin_operational_config(config_key,config_value,updated_by) VALUES('coupon-stock:"+id+"',?,?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),updated_by=VALUES(updated_by)","{\"total\":"+totalQuantity+",\"issued\":"+issued+"}",SecurityUtils.getCurrentUserId());audit("COUPON_UPDATED","COUPON",id,reason);
 }
 @Transactional public void deactivateCoupon(long id){permission.requireReviewer();store.jdbc().update("UPDATE lin_coupon SET active=0 WHERE id=?",id);audit("COUPON_DEACTIVATED","COUPON",id,null);}
 private void validateCoupon(Coupon r){if(r==null || r.name()==null || r.name().isBlank() || r.name().length()>100 || r.amount()==null || r.amount().signum()<=0 || r.amount().scale()>2 || r.minimumAmount()==null || r.minimumAmount().signum()<0 || r.startsAt()==null || r.expiresAt()==null || !r.expiresAt().isAfter(r.startsAt()))throw new BusinessException("优惠券规则无效");}
 private int[] readStock(String value){try{var node=new com.fasterxml.jackson.databind.ObjectMapper().readTree(value);return new int[]{node.path("total").intValue(),node.path("issued").intValue()};}catch(Exception e){throw new BusinessException("优惠券库存配置无效");}}
 private void audit(String action,String type,long id,String requestKey){store.jdbc().update("INSERT INTO lin_audit_record(actor_user_id,action,resource_type,resource_id,request_id) VALUES(?,?,?,?,?)",SecurityUtils.getCurrentUserId(),action,type,String.valueOf(id),requestKey);}
}
