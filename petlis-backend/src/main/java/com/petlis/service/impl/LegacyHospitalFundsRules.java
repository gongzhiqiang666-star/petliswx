package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Calculation from admin/model/basedata/Infohospital_money.php beforeInsert.
 * Deliberately separate from personal-wallet calculations. Persistence, actor mapping and gates belong to the service.
 */
final class LegacyHospitalFundsRules {
 private LegacyHospitalFundsRules(){}
 static BigDecimal balanceAfter(BigDecimal before,BigDecimal delta,String hospitalName){
  if(delta==null||delta.signum()==0)throw new BusinessException(400,"金额不允许为空");
  if(before==null)throw new BusinessException(409,"医院余额为空，请先核实原账户数据");
  // bcadd(amount, delta, 2): sum first, then truncate toward zero, not HALF_UP.
  BigDecimal after=before.add(delta).setScale(2,RoundingMode.DOWN);
  if(delta.signum()<0&&after.signum()<0)throw new BusinessException(400,"医院"+hospitalName+"可用余额不足");
  return after;
 }
}
