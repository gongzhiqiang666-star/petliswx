package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import com.petlis.common.security.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Read-only hospital pool for explicitly scoped hospital administrators. Never merges personal money. */
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class HospitalWalletService {
 private final FeatureStore store;private final AccessService access;
 private boolean enabled(){
  var rows=store.jdbc().queryForList("SELECT JSON_TYPE(JSON_EXTRACT(config_value,'$.enabled')) AS flagType,JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) AS flag FROM lin_operational_config WHERE config_key='legacy-hospital-funds' AND scope_type='GLOBAL' AND scope_id=0 AND status='ACTIVE'");
  return rows.size()==1&&"BOOLEAN".equals(rows.get(0).get("flagType"))&&"true".equals(rows.get(0).get("flag"));
 }
 public List<Map<String,Object>> balances(){
  if(!enabled())return List.of();var ids=new TreeSet<>(access.managedHospitalIds());if(ids.isEmpty())return List.of();
  String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
  return store.jdbc().queryForList("SELECT InfoHospitalId AS hospitalId,HospitalName AS hospitalName,amount AS balance,'HOSPITAL' AS fundPool FROM lin_lb_infohospital WHERE InfoHospitalId IN ("+placeholders+") ORDER BY InfoHospitalId",ids.toArray());
 }
 public Map<String,Object> transactions(int hospitalId,int page,int size,String category){
  if(!enabled())throw new BusinessException(409,"医院资金迁移模块尚未开启");
  if(!access.managedHospitalIds().contains(hospitalId))throw new BusinessException(403,"无权访问该医院资金");
  if(page<0||size<1||size>100||!Set.of("all","income","expense").contains(category==null?"":category))throw new BusinessException(400,"流水查询条件无效");
  String filter="income".equals(category)?" AND amount>0":"expense".equals(category)?" AND amount<0":"";
  return store.page("SELECT COUNT(*) FROM lin_infohospital_money WHERE infohospital_id=?"+filter,"SELECT id,amount,remark AS memo,admin_id AS adminId,createtime AS createdAt FROM lin_infohospital_money WHERE infohospital_id=?"+filter+" ORDER BY id DESC",page,size,hospitalId);
 }
}
