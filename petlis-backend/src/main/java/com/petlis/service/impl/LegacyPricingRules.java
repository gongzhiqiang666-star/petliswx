package com.petlis.service.impl;
import com.petlis.common.exception.BusinessException;
import java.math.*;
import java.util.regex.*;

/** Original PHP helpers and Base.price arithmetic. Money arithmetic never uses Double.
 * Display alone reproduces the original IEEE-754 sprintf rounding using exact integer ratios.
 */
final class LegacyPricingRules {
 private LegacyPricingRules(){}
 private static final Pattern NUMBER=Pattern.compile("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)");
 private static final Pattern INTEGER_PREFIX=Pattern.compile("^[\\x00-\\x20]*([+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)");
 static Object originalDiscount(Object value){
  if("原价".equals(value)||numericZero(value))return 10;
  if("其它折扣".equals(value))return -1;
  return value;
 }
 private static boolean numericZero(Object value){return value instanceof Number&&new BigDecimal(value.toString()).signum()==0;}
 static long discountToInt(Object value){
  if("原价".equals(value)||"其它折扣".equals(value)||numericZero(value))return 10;
  if(value==null)return 0;var m=INTEGER_PREFIX.matcher(value.toString());if(!m.find())return 0;
  try{var number=new BigDecimal(m.group(1));if(number.compareTo(BigDecimal.valueOf(Long.MAX_VALUE))>0)return Long.MAX_VALUE;if(number.compareTo(BigDecimal.valueOf(Long.MIN_VALUE))<0)return Long.MIN_VALUE;return number.longValue();}catch(NumberFormatException e){throw invalid();}
 }
 static String quote(String price,String discount){
  BigDecimal amount=decimal(price);
  if("原价".equals(discount)||"其他折扣".equals(discount))return display(amount.toPlainString());
  BigDecimal rate=decimal(discount);if(rate.compareTo(BigDecimal.TEN)==0)return display(amount.toPlainString());
  // Do not combine these two truncations: PHP bcdiv(discount,10,2), then bcmul(factor,price,2).
  var factor=rate.divide(BigDecimal.TEN,2,RoundingMode.DOWN);
  var result=factor.multiply(amount).setScale(2,RoundingMode.DOWN);
  return display(result.toPlainString());
 }
 static String display(String input){
  BigDecimal value=decimal(input);if(value.signum()==0)return "0.00";
  BigDecimal magnitude=value.abs();BigInteger numerator=magnitude.unscaledValue(),denominator=BigInteger.ONE;
  if(magnitude.scale()>=0)denominator=BigInteger.TEN.pow(magnitude.scale());else numerator=numerator.multiply(BigInteger.TEN.pow(-magnitude.scale()));
  int exponent=numerator.bitLength()-denominator.bitLength();
  int comparison=exponent>=0?numerator.compareTo(denominator.shiftLeft(exponent)):numerator.shiftLeft(-exponent).compareTo(denominator);
  if(comparison<0)exponent--;
  int shift=52-exponent;BigInteger n=shift>=0?numerator.shiftLeft(shift):numerator,d=shift>=0?denominator:denominator.shiftLeft(-shift);
  var division=n.divideAndRemainder(d);BigInteger mantissa=division[0];int halfway=division[1].shiftLeft(1).compareTo(d);
  if(halfway>0||halfway==0&&mantissa.testBit(0))mantissa=mantissa.add(BigInteger.ONE);
  int power=exponent-52;
  BigDecimal binary=power>=0?new BigDecimal(mantissa.shiftLeft(power)):new BigDecimal(mantissa).divide(new BigDecimal(BigInteger.ONE.shiftLeft(-power)));
  if(value.signum()<0)binary=binary.negate();BigDecimal rounded=binary.setScale(2,RoundingMode.HALF_EVEN);
  if(value.signum()<0&&rounded.signum()==0)return "-0.00";
  return rounded.toPlainString();
 }
 private static BigDecimal decimal(String value){if(value==null||value.length()>128||!NUMBER.matcher(value.trim()).matches())throw invalid();try{return new BigDecimal(value.trim());}catch(NumberFormatException e){throw invalid();}}
 private static BusinessException invalid(){return new BusinessException(400,"价格或折扣格式无效");}
}
