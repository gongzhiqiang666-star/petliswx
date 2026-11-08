package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyHospitalFundsRulesTest {
 @Test void creditAndDebitRemainHospitalPoolAmounts(){
  assertEquals(new BigDecimal("12.50"),LegacyHospitalFundsRules.balanceAfter(new BigDecimal("10.00"),new BigDecimal("2.50"),"医院A"));
  assertEquals(new BigDecimal("0.00"),LegacyHospitalFundsRules.balanceAfter(new BigDecimal("10.00"),new BigDecimal("-10.00"),"医院A"));
 }
 @Test void zeroAndInsufficientFundsKeepOriginalMessages(){
  assertEquals("金额不允许为空",assertThrows(BusinessException.class,()->LegacyHospitalFundsRules.balanceAfter(BigDecimal.TEN,BigDecimal.ZERO,"医院A")).getMessage());
  assertEquals("医院医院A可用余额不足",assertThrows(BusinessException.class,()->LegacyHospitalFundsRules.balanceAfter(BigDecimal.TEN,new BigDecimal("-10.01"),"医院A")).getMessage());
 }
 @Test void phpBcaddScaleIsAppliedAfterSumNotBeforeOperands(){
  assertEquals(new BigDecimal("10.00"),LegacyHospitalFundsRules.balanceAfter(new BigDecimal("10.00"),new BigDecimal("0.009"),"A"));
  assertEquals(new BigDecimal("0.00"),LegacyHospitalFundsRules.balanceAfter(new BigDecimal("0.00"),new BigDecimal("-0.009"),"A"));
  assertEquals(new BigDecimal("1.01"),LegacyHospitalFundsRules.balanceAfter(new BigDecimal("1.009"),new BigDecimal("0.009"),"A"));
 }
 @Test void existingNegativeBalanceCanStillBeCreditedAsInPhp(){
  assertEquals(new BigDecimal("-1.99"),LegacyHospitalFundsRules.balanceAfter(new BigDecimal("-2.00"),new BigDecimal("0.01"),"A"));
 }
}
